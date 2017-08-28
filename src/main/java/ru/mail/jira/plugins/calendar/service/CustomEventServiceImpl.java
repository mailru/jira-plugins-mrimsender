package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.ImmutableSet;
import net.java.ao.DBParam;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.model.*;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.rest.dto.*;
import ru.mail.jira.plugins.calendar.service.recurrent.generation.ChronoUnitDateGenerator;
import ru.mail.jira.plugins.calendar.service.recurrent.generation.CronDateGenerator;
import ru.mail.jira.plugins.calendar.service.recurrent.generation.DateGenerator;
import ru.mail.jira.plugins.calendar.service.recurrent.generation.DaysOfWeekDateGenerator;
import ru.mail.jira.plugins.commons.RestFieldException;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CustomEventServiceImpl implements CustomEventService {
    private static final Set<String> SUPPORTED_AVATAR_NAMES = ImmutableSet.of("event", "travel", "birthday", "leave");
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");
    private static final long GENERATION_LIMIT_PER_REQUEST = 1000;

    private final Logger logger = LoggerFactory.getLogger(CustomEventServiceImpl.class);
    private final ActiveObjects ao;
    private final I18nResolver i18nResolver;
    private final JiraDeprecatedService jiraDeprecatedService;
    private final UserManager userManager;
    private final AvatarService avatarService;
    private final CalendarService calendarService;
    private final PermissionService permissionService;
    private final TimeZoneManager timeZoneManager;

    @Autowired
    public CustomEventServiceImpl(
        @ComponentImport I18nResolver i18nResolver,
        @ComponentImport UserManager userManager,
        @ComponentImport AvatarService avatarService,
        @ComponentImport ActiveObjects ao,
        @ComponentImport TimeZoneManager timeZoneManager,
        JiraDeprecatedService jiraDeprecatedService,
        CalendarService calendarService,
        PermissionService permissionService
    ) {
        this.ao = ao;
        this.i18nResolver = i18nResolver;
        this.jiraDeprecatedService = jiraDeprecatedService;
        this.userManager = userManager;
        this.avatarService = avatarService;
        this.calendarService = calendarService;
        this.permissionService = permissionService;
        this.timeZoneManager = timeZoneManager;
    }

    @Override
    public EventDto createEvent(ApplicationUser user, CustomEventDto eventDto) throws GetException {
        preValidate(user, eventDto);

        EventType eventType = getEventType(eventDto.getEventTypeId());
        Calendar calendar = calendarService.getCalendar(eventDto.getCalendarId());

        Event parent = null;

        if (eventDto.getParentId() != null) {
            parent = getEvent(eventDto.getParentId());
        }

        if (!permissionService.hasEditEventsPermission(user, calendar)) {
            throw new SecurityException("No permission to create events");
        }

        if (eventType.isDeleted()) {
            throw new IllegalArgumentException(i18nResolver.getText("ru.mail.jira.plguins.calendar.customEvents.eventTypeIsDeleted"));
        }

        validateAndNormalize(eventDto, null, parent, calendar, eventType);

        RecurrenceType recurrenceType = null;
        Integer recurrencePeriod = null;
        String recurrenceExpression = null;
        Timestamp recurrenceEndDate = null;
        Integer recurrenceCount = null;

        if (eventDto.getRecurrenceType() != null) {
            recurrenceType = RecurrenceType.valueOf(eventDto.getRecurrenceType());
            recurrencePeriod = eventDto.getRecurrencePeriod();
            recurrenceExpression = eventDto.getRecurrenceExpression();
            recurrenceEndDate = eventDto.getRecurrenceEndDate();
            recurrenceCount = eventDto.getRecurrenceCount();
        }

        if (parent != null) {
            //delete event with same parent & recurrence number, since we are unable to create unique index (parent_id, recurrence_number)
            ao.deleteWithSQL(Event.class, "PARENT_ID = ? AND RECURRENCE_NUMBER = ?", parent.getID(), eventDto.getRecurrenceNumber());
        }

        Event event = ao.create(
            Event.class,
            new DBParam("TITLE", eventDto.getTitle()),
            new DBParam("CALENDAR_ID", eventDto.getCalendarId()),
            new DBParam("START_DATE", eventDto.getStartDate()),
            new DBParam("END_DATE", eventDto.getEndDate()),
            new DBParam("EVENT_TYPE_ID", eventType.getID()),
            new DBParam("CREATOR_KEY", user.getKey()),
            new DBParam("PARTICIPANTS", eventDto.getParticipantNames()),
            new DBParam("ALL_DAY", eventDto.isAllDay()),
            new DBParam("RECURRENCE_TYPE", recurrenceType),
            new DBParam("RECURRENCE_PERIOD", recurrencePeriod),
            new DBParam("RECURRENCE_EXPRESSION", recurrenceExpression),
            new DBParam("RECURRENCE_END_DATE", recurrenceEndDate),
            new DBParam("RECURRENCE_COUNT", recurrenceCount),
            new DBParam("PARENT_ID", eventDto.getParentId()),
            new DBParam("RECURRENCE_NUMBER", eventDto.getRecurrenceNumber())
        );

        if (eventDto.getParentId() != null) {
            return buildRecurrentEvent(parent, eventDto.getRecurrenceNumber(), event, null, null, calendar, user, true);
        } else {
            return buildEvent(user, event, calendar, true);
        }
    }

    @Override
    public EventDto editEvent(ApplicationUser user, CustomEventDto eventDto) throws GetException {
        preValidate(user, eventDto);

        Event event = getEvent(eventDto.getId());
        Calendar calendar = calendarService.getCalendar(event.getCalendarId());

        if (!permissionService.hasEditEventsPermission(user, calendar)) {
            throw new SecurityException("No permission to edit event");
        }

        EventType eventType = getEventType(eventDto.getEventTypeId());

        validateAndNormalize(eventDto, event, event.getParent(), calendar, eventType);

        EditMode editMode = eventDto.getEditMode();

        //keep type if deleted, but not modified in event
        if (eventType.isDeleted() && eventDto.getEventTypeId() != event.getEventType().getID()) {
            throw new IllegalArgumentException(i18nResolver.getText("ru.mail.jira.plguins.calendar.customEvents.eventTypeIsDeleted"));
        }

        if (event.getParent() != null && (editMode == EditMode.ALL_EVENTS || editMode == EditMode.FOLLOWING_EVENTS)) {
            event = event.getParent();
        }

        RecurrenceType recurrenceType = null;
        Integer recurrencePeriod = null;
        String recurrenceExpression = null;
        Timestamp recurrenceEndDate = null;
        Integer recurrenceCount = null;

        if (eventDto.getRecurrenceType() != null) {
            recurrenceType = RecurrenceType.valueOf(eventDto.getRecurrenceType());
            recurrencePeriod = eventDto.getRecurrencePeriod();
            recurrenceExpression = eventDto.getRecurrenceExpression();
            recurrenceEndDate = eventDto.getRecurrenceEndDate();
            recurrenceCount = eventDto.getRecurrenceCount();
        }

        if (editMode == EditMode.FOLLOWING_EVENTS) {
            Timestamp startDate = eventDto.getStartDate();
            ao.deleteWithSQL(Event.class, "PARENT_ID = ? AND START_DATE >= ?", event.getID(), startDate);

            event.setRecurrenceEndDate(startDate);
            event.save();

            event = ao.create(
                Event.class,
                new DBParam("TITLE", eventDto.getTitle()),
                new DBParam("CALENDAR_ID", eventDto.getCalendarId()),
                new DBParam("START_DATE", eventDto.getStartDate()),
                new DBParam("END_DATE", eventDto.getEndDate()),
                new DBParam("EVENT_TYPE_ID", eventType.getID()),
                new DBParam("CREATOR_KEY", user.getKey()),
                new DBParam("PARTICIPANTS", eventDto.getParticipantNames()),
                new DBParam("ALL_DAY", eventDto.isAllDay()),
                new DBParam("RECURRENCE_TYPE", recurrenceType),
                new DBParam("RECURRENCE_PERIOD", recurrencePeriod),
                new DBParam("RECURRENCE_EXPRESSION", recurrenceExpression),
                new DBParam("RECURRENCE_END_DATE", recurrenceEndDate),
                new DBParam("RECURRENCE_COUNT", recurrenceCount),
                new DBParam("PARENT_ID", null),
                new DBParam("RECURRENCE_NUMBER", null)
            );

            return buildEvent(user, event, calendar, true);
        }

        if (
            !Objects.equals(recurrenceType, event.getRecurrenceType()) ||
            !Objects.equals(recurrencePeriod, event.getRecurrencePeriod()) ||
            !Objects.equals(recurrenceExpression, event.getRecurrenceExpression())
        ) {
            ao.deleteWithSQL(Event.class, "PARENT_ID = ?", event.getID());
        } else if (recurrenceType != null) {
            if (recurrenceEndDate != null) {
                ao.deleteWithSQL(Event.class, "PARENT_ID = ? AND START_DATE > ?", event.getID(), recurrenceEndDate);
            }
            if (recurrenceCount != null) {
                ao.deleteWithSQL(Event.class, "PARENT_ID = ? AND RECURRENCE_NUMBER > ?", event.getID(), recurrenceCount);
            }
        }

        event.setStartDate(eventDto.getStartDate());
        event.setEndDate(eventDto.getEndDate());
        event.setTitle(eventDto.getTitle());
        event.setEventType(eventType);
        event.setParticipants(eventDto.getParticipantNames());
        event.setAllDay(eventDto.isAllDay());
        event.setRecurrenceType(recurrenceType);
        event.setRecurrencePeriod(recurrencePeriod);
        event.setRecurrenceExpression(recurrenceExpression);
        event.setRecurrenceEndDate(recurrenceEndDate);
        event.setRecurrenceCount(recurrenceCount);
        event.save();

        if (event.getParent() != null && event.getRecurrenceNumber() != null) {
            return buildRecurrentEvent(event.getParent(), event.getRecurrenceNumber(), event, null, null, calendar, user, true);
        } else {
            return buildEvent(user, event, calendar, true);
        }
    }

    @Override
    public void deleteEvent(ApplicationUser user, int eventId) throws GetException {
        Event event = getEvent(eventId);
        Calendar calendar = calendarService.getCalendar(event.getCalendarId());

        if (!permissionService.hasEditEventsPermission(user, calendar)) {
            throw new SecurityException("No permission to edit event");
        }

        ao.deleteWithSQL(Event.class, "PARENT_ID = ?", eventId);
        ao.delete(event);
    }

    @Override
    public EventDto moveEvent(ApplicationUser user, int id, EventMoveDto moveDto) throws GetException {
        Event event = getEvent(id);

        Calendar calendar = calendarService.getCalendar(event.getCalendarId());

        if (!permissionService.hasEditEventsPermission(user, calendar)) {
            throw new SecurityException("No permission to view event");
        }

        validateDates(moveDto.getStart(), moveDto.getEnd());

        EditMode editMode = moveDto.getEditMode();

        if (editMode == EditMode.SINGLE_EVENT) {
            if (moveDto.getParentId() != null) {
                if (event.getParent() != null) {
                    throw new IllegalArgumentException("event has parent");
                }
                if (moveDto.getRecurrenceNumber() == null) {
                    throw new IllegalArgumentException("recurrence number is null");
                }

                Event parent = getEvent(moveDto.getParentId());

                //delete event with same parent & recurrence number, since we are unable to create unique index (parent_id, recurrence_number)
                ao.deleteWithSQL(Event.class, "PARENT_ID = ? AND RECURRENCE_NUMBER = ?", parent.getID(), moveDto.getRecurrenceNumber());

                event = ao.create(
                    Event.class,
                    new DBParam("TITLE", parent.getTitle()),
                    new DBParam("CALENDAR_ID", parent.getCalendarId()),
                    new DBParam("START_DATE", moveDto.getStart()),
                    new DBParam("END_DATE", moveDto.getEnd()),
                    new DBParam("EVENT_TYPE_ID", parent.getEventType().getID()),
                    new DBParam("CREATOR_KEY", user.getKey()),
                    new DBParam("PARTICIPANTS", parent.getParticipants()),
                    new DBParam("ALL_DAY", moveDto.isAllDay()),
                    new DBParam("RECURRENCE_TYPE", null),
                    new DBParam("RECURRENCE_PERIOD", null),
                    new DBParam("RECURRENCE_EXPRESSION", null),
                    new DBParam("RECURRENCE_END_DATE", null),
                    new DBParam("RECURRENCE_COUNT", null),
                    new DBParam("PARENT_ID", parent.getID()),
                    new DBParam("RECURRENCE_NUMBER", moveDto.getRecurrenceNumber())
                );
                return buildRecurrentEvent(parent, event.getRecurrenceNumber(), event, null, null, calendar, user, true);
            } else {
                event.setAllDay(moveDto.isAllDay());
                event.setStartDate(moveDto.getStart());
                event.setEndDate(moveDto.getEnd());
                event.save();
            }
        } else if (editMode == EditMode.FOLLOWING_EVENTS) {
            Timestamp startDate = moveDto.getStart();

            Event originalEvent = event;

            event = ao.create(
                Event.class,
                new DBParam("TITLE", event.getTitle()),
                new DBParam("CALENDAR_ID", event.getCalendarId()),
                new DBParam("START_DATE", startDate),
                new DBParam("END_DATE", moveDto.getEnd()),
                new DBParam("ALL_DAY", moveDto.isAllDay()),
                new DBParam("EVENT_TYPE_ID", event.getEventType().getID()),
                new DBParam("CREATOR_KEY", user.getKey()),
                new DBParam("PARTICIPANTS", event.getParticipants()),
                new DBParam("RECURRENCE_TYPE", event.getRecurrenceType()),
                new DBParam("RECURRENCE_PERIOD", event.getRecurrencePeriod()),
                new DBParam("RECURRENCE_EXPRESSION", event.getRecurrenceExpression()),
                new DBParam("RECURRENCE_END_DATE", event.getRecurrenceEndDate()),
                new DBParam("RECURRENCE_COUNT", event.getRecurrenceCount()),
                new DBParam("PARENT_ID", null),
                new DBParam("RECURRENCE_NUMBER", null)
            );

            ao.deleteWithSQL(Event.class, "PARENT_ID = ? AND START_DATE >= ?", originalEvent.getID(), startDate);
            originalEvent.setRecurrenceEndDate(startDate);
            originalEvent.save();

            return buildEvent(user, event, calendar, true);
        } else if (editMode == EditMode.ALL_EVENTS) {
            if (event.getParent() != null || event.getRecurrenceType() == null) {
                throw new IllegalArgumentException("wrong event");
            }
            event.setAllDay(moveDto.isAllDay());
            event.setStartDate(moveDto.getStart());
            event.setEndDate(moveDto.getEnd());
            event.save();
        }

        if (event.getParent() != null && event.getRecurrenceNumber() != null) {
            return buildRecurrentEvent(event.getParent(), event.getRecurrenceNumber(), event, null, null, calendar, user, true);
        } else {
            return buildEvent(user, event, calendar, true);
        }
    }

    @Override
    public int getEventCount(ApplicationUser user, int calendarId) throws GetException {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!permissionService.hasUsePermission(user, calendar) && !permissionService.hasAdminPermission(user, calendar)) {
            throw new SecurityException("No permission to view events");
        }

        return ao.count(Event.class, Query.select().where("CALENDAR_ID = ?", calendarId));
    }

    @Override
    public CustomEventDto getEventDto(ApplicationUser user, int id) throws GetException {
        Event event = getEvent(id);
        Event recurringEvent = event;

        Calendar calendar = calendarService.getCalendar(event.getCalendarId());

        boolean hasEditPermission = permissionService.hasEditEventsPermission(user, calendar);
        if (!permissionService.hasUsePermission(user, calendar) && !hasEditPermission) {
            throw new SecurityException("No permission to view event");
        }

        CustomEventDto result = new CustomEventDto();
        EventType eventType = event.getEventType();

        if (event.getParent() != null) {
            Event parent = event.getParent();
            recurringEvent = event.getParent();

            result.setParentStartDate(parent.getStartDate());
            result.setParentEndDate(parent.getEndDate());
            result.setParentAllDay(parent.isAllDay());
            result.setParentId(parent.getID());
        }

        result.setId(event.getID());
        result.setTitle(event.getTitle());
        result.setCalendarId(calendar.getID());
        result.setCalendarName(calendar.getName());
        result.setEndDate(event.getEndDate());
        result.setStartDate(event.getStartDate());
        result.setEventTypeId(eventType.getID());
        result.setEventTypeName(getTypeDisplayName(eventType));
        result.setEventTypeAvatar(eventType.getAvatar());
        result.setEditable(hasEditPermission);
        result.setAllDay(event.isAllDay());

        RecurrenceType recurrenceType = recurringEvent.getRecurrenceType();
        if (recurrenceType != null) {
            result.setRecurrenceType(recurrenceType.name());
            result.setRecurrenceExpression(recurringEvent.getRecurrenceExpression());
            result.setRecurrencePeriod(recurringEvent.getRecurrencePeriod());
            result.setRecurrenceCount(recurringEvent.getRecurrenceCount());
            result.setRecurrenceEndDate(recurringEvent.getRecurrenceEndDate());
        }

        EventTypeReminder reminder = getEventReminderOption(eventType.getID(), calendar.getID());
        if (reminder != null) {
            result.setReminder(reminder.getReminderType().name());
        }

        List<UserDto> participants = parseParticipants(event.getParticipants());
        result.setParticipants(participants);
        if (participants != null) {
            result.setParticipantNames(participants.stream().map(UserDto::getName).collect(Collectors.joining(", ")));
        }

        return result;
    }

    private void preValidate(ApplicationUser user, CustomEventDto eventDto) {
        if (user == null) {
            throw new IllegalArgumentException("User doesn't exist");
        }

        if (StringUtils.isBlank(eventDto.getTitle())) {
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("common.words.name")), "title");
        }

        if (eventDto.getCalendarId() == 0) {
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.dialog.calendar")), "calendar");
        }

        if (eventDto.getEventTypeId() == 0) {
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.type")), "type");
        }
    }

    private void validateAndNormalize(CustomEventDto eventDto, Event event, Event parent, Calendar calendar, EventType eventType) {
        if (eventDto.getEditMode() == null) {
            throw new IllegalArgumentException("editMode is empty");
        }

        Timestamp startDate = eventDto.getStartDate();
        Timestamp endDate = eventDto.getEndDate();

        validateDates(startDate, endDate);

        if (!eventType.isSystem() && eventType.getCalendarId() != null && eventType.getCalendarId() != calendar.getID()) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.dialog.error.incorrectType"), "type");
        }

        String participants = StringUtils.trimToNull(eventDto.getParticipantNames());

        List<String> keys = new ArrayList<>();
        if (participants != null) {
            String[] participantNames = participants.split("\\s*,\\s*");
            for (String participantName : participantNames) {
                ApplicationUser participant = userManager.getUserByName(participantName);

                if (participant == null) {
                    throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.dialog.error.unknownUser", participantName), "participantNames");
                }

                keys.add(participant.getKey());
            }
        }

        EditMode editMode = eventDto.getEditMode();
        if (editMode == EditMode.SINGLE_EVENT) {
            if (parent != null) {
                eventDto.setRecurrenceType(null);

                Timestamp recurrenceEndDate = parent.getRecurrenceEndDate();
                if (recurrenceEndDate != null && eventDto.getStartDate().after(recurrenceEndDate)) {
                    throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.recurring.error.startDateAfterRecurrenceEnd"), "startDate");
                }
            }
        } else if (editMode == EditMode.FOLLOWING_EVENTS) {
            Event e = parent != null ? parent : event;
            if (!e.getStartDate().before(eventDto.getStartDate())) {
                throw new RestFieldException("ru.mail.jira.plugins.calendar.customEvents.recurring.error.startDateBeforeOldStart", "startDate");
            }
        }

        if (event == null && parent != null) {
            if (eventDto.getRecurrenceNumber() == null) {
                throw new IllegalArgumentException("Recurrence number is required");
            }

            if (parent.getRecurrenceType() == null) {
                throw new IllegalArgumentException("Parent event must be recurrent");
            }
        }

        String recurrenceTypeString = StringUtils.trimToNull(eventDto.getRecurrenceType());
        eventDto.setRecurrenceType(recurrenceTypeString);

        if (recurrenceTypeString != null) {
            RecurrenceType recurrenceType;
            try {
                recurrenceType = RecurrenceType.valueOf(recurrenceTypeString);
            } catch (IllegalArgumentException e) {
                throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.dialog.error.unknownRecurrenceType"), "recurrenceType");
            }

            Integer recurrenceCount = eventDto.getRecurrenceCount();
            Timestamp recurrenceEndDate = eventDto.getRecurrenceEndDate();

            if (recurrenceCount != null && recurrenceEndDate != null) {
                throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.recurring.error.countAndEndDate"), "recurrenceEnd");
            }

            if (recurrenceCount != null) {
                if (recurrenceCount <= 0) {
                    throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.recurring.error.countPositive"), "recurrenceCount");
                }
            }

            if (recurrenceEndDate != null) {
                if (recurrenceEndDate.before(eventDto.getStartDate())) {
                    throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.recurring.error.endDateBeforeStart"), "recurrenceEndDate");
                }
            }
            recurrenceType.getValidator().validateDto(i18nResolver, eventDto);
        }

        if (keys.size() > 0) {
            eventDto.setParticipantNames(keys.stream().collect(Collectors.joining(", ")));
        } else {
            eventDto.setParticipantNames(null);
        }
    }

    private void validateDates(Timestamp startDate, Timestamp endDate) {
        if (startDate == null) {
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("ru.mail.jira.plugins.calendar.dialog.eventStart")), "startDate");
        }

        if (endDate != null && startDate.after(endDate)) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.dialog.error.endDateBeforeStart"), "endDate");
        }
    }

    private Event getEvent(int id) throws GetException {
        Event event = ao.get(Event.class, id);
        if (event == null)
            throw new GetException("No Event with id=" + id);
        return event;
    }

    private EventType getEventType(int id) throws GetException {
        EventType eventType = ao.get(EventType.class, id);
        if (eventType == null) {
            throw new GetException("No EventType with id=" + id);
        }
        return eventType;
    }

    private EventTypeReminder getEventReminderOption(int eventTypeId, int calendarId) {
        EventTypeReminder[] result = ao.find(EventTypeReminder.class, Query.select().where("EVENT_TYPE_ID = ? AND CALENDAR_ID = ?", eventTypeId, calendarId));
        if (result.length > 0) {
            return result[0];
        } else {
            return null;
        }
    }

    @Override
    public List<EventDto> getEvents(ApplicationUser user, Calendar calendar, Date start, Date end) {
        List<EventDto> result = new ArrayList<>();
        Event[] customEvents = ao.find(
            Event.class,
            Query
                .select()
                .where(
                    "(START_DATE >= ? AND (END_DATE <= ? OR END_DATE IS NULL AND START_DATE <= ?) OR (START_DATE <= ? AND END_DATE >= ?) OR (START_DATE <= ? AND END_DATE >= ?)) AND CALENDAR_ID = ? AND RECURRENCE_TYPE IS NULL AND PARENT_ID IS NULL",
                    start, end, end, start, start, end, end, calendar.getID()
                )
        );
        boolean canEditEvents = permissionService.hasEditEventsPermission(user, calendar);
        for (Event customEvent : customEvents) {
            result.add(buildEvent(user, customEvent, calendar, canEditEvents));
        }
        result.addAll(collectRecurringEvents(user, calendar, start, end));
        return result;
    }

    private List<EventDto> collectRecurringEvents(ApplicationUser user, Calendar calendar, Date start, Date end) {
        Event[] recurringEvents = ao.find(
            Event.class,
            Query
                .select()
                .where(
                    "(START_DATE <= ?) AND CALENDAR_ID = ? AND RECURRENCE_TYPE IS NOT NULL",
                    end, calendar.getID()
                )
        );

        boolean canEditEvents = permissionService.hasEditEventsPermission(user, calendar);
        ZoneId zoneId = timeZoneManager.getTimeZoneforUser(user).toZoneId();

        return Arrays
            .stream(recurringEvents)
            .flatMap(event -> generateRecurringEvents(
                    event,
                    calendar,
                    user, canEditEvents,
                    start.toInstant().atZone(zoneId), end.toInstant().atZone(zoneId),
                    event.isAllDay() ? UTC_TZ.toZoneId() : zoneId
                )
                .stream()
            )
            .collect(Collectors.toList());
    }

    private List<EventDto> generateRecurringEvents(Event event, Calendar calendar, ApplicationUser user, boolean canEditEvents, ZonedDateTime since, ZonedDateTime until, ZoneId zoneId) {
        DateGenerator dateGenerator = getDateGenerator(event, zoneId);

        Map<Integer, Event> children = Arrays
            .stream(getChildEvents(Date.from(since.toInstant()), Date.from(until.toInstant()), event))
            .collect(Collectors.toMap(
                Event::getRecurrenceNumber,
                Function.identity(),
                (a, b) -> {
                    logger.warn(
                        "found events ({}, {}) with same parent ({}) and recurrent number ({})",
                        a.getID(), b.getID(), event.getID(), a.getRecurrenceNumber()
                    );
                    if (a.getID() > b.getID()) {
                        return a;
                    }
                    return b;
                }
            ));

        if (dateGenerator == null) {
            logger.error("Unable to create date generator for event {} with recurrence type {}", event.getID(), event.getRecurrenceType().name());
            return new ArrayList<>();
        }

        ZonedDateTime startDate = dateGenerator.nextDate();

        Long duration = null;
        ZonedDateTime endDate = null;
        if (event.getEndDate() != null) {
            duration = event.getEndDate().getTime() - event.getStartDate().getTime();
            endDate = event.getEndDate().toInstant().atZone(zoneId); //get initial end date from event
        }

        Integer recurrenceCount = event.getRecurrenceCount();
        ZonedDateTime recurrenceEndDate = null;
        if (event.getRecurrenceEndDate() != null) {
            recurrenceEndDate = event.getRecurrenceEndDate().toInstant().atZone(zoneId);
        }

        List<EventDto> result = new ArrayList<>();
        int number = 0;

        while (startDate.isBefore(until) && isBeforeEnd(startDate, recurrenceEndDate) && isCountOk(number, recurrenceCount)) {
            if (startDate.isAfter(since) || endDate != null && endDate.isAfter(since)) {
                result.add(buildRecurrentEvent(event, number, children.get(number), startDate, endDate, calendar, user, canEditEvents));
            }

            number++;
            startDate = dateGenerator.nextDate();
            if (duration != null) {
                endDate = startDate.plus(duration, ChronoUnit.MILLIS);
            }

            if (isLimitExceeded(result.size())) {
                break;
            }
        }

        return result;
    }

    @Override
    public EventTypeDto createEventType(ApplicationUser user, EventTypeDto typeDto) throws GetException {
        Calendar calendar = calendarService.getCalendar(typeDto.getCalendarId());

        if (!permissionService.hasAdminPermission(user, calendar)) {
            throw new SecurityException("No permission to manage event types");
        }

        validateEventType(user, typeDto);

        ReminderType reminderType = typeDto.getReminder() != null ? ReminderType.valueOf(typeDto.getReminder()) : null;

        EventType eventType = ao.create(
            EventType.class,
            new DBParam("NAME", typeDto.getName()),
            new DBParam("AVATAR", typeDto.getAvatar()),
            new DBParam("CALENDAR_ID", typeDto.getCalendarId()),
            new DBParam("DELETED", Boolean.FALSE),
            new DBParam("SYSTEM", Boolean.FALSE)
        );

        EventTypeReminder reminder = null;

        if (reminderType != null) {
            reminder = ao.create(
                EventTypeReminder.class,
                new DBParam("EVENT_TYPE_ID", eventType.getID()),
                new DBParam("CALENDAR_ID", typeDto.getCalendarId()),
                new DBParam("REMINDER_TYPE", reminderType)
            );
        }

        return buildEventType(eventType, reminder);
    }

    private Event[] getChildEvents(Date start, Date end, Event event) {
        return ao.find(
            Event.class,
            Query
                .select()
                .where(
                    "(START_DATE >= ? AND (END_DATE <= ? OR END_DATE IS NULL AND START_DATE <= ?) OR (START_DATE <= ? AND END_DATE >= ?) OR (START_DATE <= ? AND END_DATE >= ?)) AND PARENT_ID = ? AND RECURRENCE_TYPE IS NULL",
                    start, end, end, start, start, end, end, event.getID()
                )
        );
    }

    @Override
    public EventTypeDto editEventType(ApplicationUser user, EventTypeDto typeDto) throws GetException {
        EventType eventType = getEventType(typeDto.getId());
        Calendar calendar = eventType.isSystem() ?
            calendarService.getCalendar(typeDto.getCalendarId()):
            calendarService.getCalendar(eventType.getCalendarId());

        if (!permissionService.hasAdminPermission(user, calendar)) {
            throw new SecurityException("No permission to manage event types");
        }

        if (eventType.isSystem()) {
            validateReminder(typeDto);
        } else {
            validateEventType(user, typeDto);
        }

        ReminderType reminderType = typeDto.getReminder() != null ? ReminderType.valueOf(typeDto.getReminder()) : null;
        EventTypeReminder reminder = getEventReminderOption(eventType.getID(), calendar.getID());

        if (reminderType != null) {
            if (reminder != null) {
                reminder.setReminderType(reminderType);
                reminder.save();
            } else {
                reminder = ao.create(
                    EventTypeReminder.class,
                    new DBParam("EVENT_TYPE_ID", eventType.getID()),
                    new DBParam("CALENDAR_ID", calendar.getID()),
                    new DBParam("REMINDER_TYPE", reminderType)
                );
            }
        } else if (reminder != null) {
            ao.delete(reminder);
            reminder = null;
        }

        if (!eventType.isSystem()) {
            eventType.setAvatar(typeDto.getAvatar());
            eventType.setName(typeDto.getName());
            eventType.save();
        }

        return buildEventType(eventType, reminder);
    }

    public void deleteEventType(ApplicationUser user, int id) throws GetException {
        EventType eventType = getEventType(id);
        Calendar calendar = calendarService.getCalendar(eventType.getCalendarId());

        if (!permissionService.hasAdminPermission(user, calendar)) {
            throw new SecurityException("No permission to manage event types");
        }

        EventTypeReminder reminder = getEventReminderOption(eventType.getID(), eventType.getCalendarId());

        if (reminder != null) {
            ao.delete(reminder);
        }

        eventType.setDeleted(true);
        eventType.save();
    }

    private void validateEventType(ApplicationUser user, EventTypeDto typeDto) throws GetException {
        if (user == null) {
            throw new IllegalArgumentException("User doesn't exist");
        }

        typeDto.setName(StringUtils.trimToNull(typeDto.getName()));
        typeDto.setReminder(StringUtils.trimToNull(typeDto.getReminder()));

        if (typeDto.getName() == null) {
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("common.words.name")), "name");
        }

        if (StringUtils.isEmpty(typeDto.getAvatar())) {
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.avatar")), "avatar");
        }

        if (!SUPPORTED_AVATAR_NAMES.contains(typeDto.getAvatar())) {
            throw new RestFieldException("Unsupported avatar", "avatar");
        }

        validateReminder(typeDto);
    }

    private void validateReminder(EventTypeDto typeDto) {
        String reminderOption = typeDto.getReminder();
        if (reminderOption != null) {
            try {
                ReminderType.valueOf(reminderOption);
            } catch (IllegalArgumentException e) {
                throw new RestFieldException("Unknown reminder option", "reminder");
            }
        }
    }

    @Override
    public List<EventTypeDto> getTypes(ApplicationUser user, int calendarId) throws GetException {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!permissionService.hasUsePermission(user, calendar) && !permissionService.hasEditEventsPermission(user, calendar)) {
            throw new SecurityException("No permission to view event types");
        }

        EventType[] eventTypes = ao.find(
            EventType.class,
            Query.select().where("(CALENDAR_ID = ? OR CALENDAR_ID IS NULL) AND DELETED = ?", calendarId, Boolean.FALSE)
        );

        List<EventTypeDto> result = new ArrayList<>();

        for (EventType eventType : eventTypes) {
            result.add(buildEventType(eventType, getEventReminderOption(eventType.getID(), calendarId)));
        }

        return result;
    }

    private String getTypeDisplayName(EventType eventType) {
        if (eventType.getI18nName() != null) {
            return i18nResolver.getText(eventType.getI18nName());
        }
        return eventType.getName();
    }

    private List<UserDto> parseParticipants(String participants) {
        if (participants == null) {
            return null;
        }

        String[] participantKeys = participants.split("\\s*,\\s*");

        List<UserDto> result = new ArrayList<>();
        for (String participantKey : participantKeys) {
            ApplicationUser participant = userManager.getUserByKey(participantKey);

            if (participant != null) {
                result.add(buildUser(participant));
            } else {
                result.add(buildNonExistingUser(participantKey));
            }
        }

        return result;
    }

    private EventDto buildEvent(ApplicationUser user, Event event, Calendar calendar, boolean canEditEvents) {
        DateTimeFormatter dateFormatter = getDateFormatter(user, event.isAllDay());

        EventDto result = new EventDto();
        result.setCalendarId(event.getCalendarId());
        if (event.getRecurrenceNumber() != null && event.getParent() != null) {
            result.setId(event.getParent().getID() + "-" + event.getRecurrenceNumber());
        } else {
            result.setId(String.valueOf(-1 * event.getID()));
        }
        result.setOriginalId(String.valueOf(event.getID()));
        result.setTitle(event.getTitle());
        result.setColor(calendar.getColor());
        result.setAllDay(event.isAllDay());
        result.setIssueTypeImgUrl(event.getEventType().getAvatar());
        result.setStatus(null);
        result.setType(EventDto.Type.CUSTOM);
        result.setParticipants(parseParticipants(event.getParticipants()));
        result.setStart(dateFormatter.format(event.getStartDate()));

        Date end = event.getEndDate();
        if (end != null) {
            if (event.isAllDay()) {
                result.setEnd(dateFormatter.format(new Date(event.getEndDate().getTime() + TimeUnit.HOURS.toMillis(24))));
            } else {
                result.setEnd(dateFormatter.format(event.getEndDate()));
            }
        }

        result.setStartEditable(canEditEvents);
        result.setDurationEditable(canEditEvents);

        return result;
    }

    protected EventDto buildRecurrentEvent(
        Event event, int number, Event childEvent,
        ZonedDateTime startDate, ZonedDateTime endDate,
        Calendar calendar, ApplicationUser user, boolean canEditEvents
    ) {
        EventDto result = new EventDto();
        if (childEvent != null) {
            DateTimeFormatter dateTimeFormatter = getDateFormatter(user, childEvent.isAllDay());

            result.setTitle(childEvent.getTitle());
            result.setOriginalId(String.valueOf(childEvent.getID()));
            result.setParentId(String.valueOf(event.getID()));

            result.setAllDay(childEvent.isAllDay());
            result.setStart(dateTimeFormatter.format(childEvent.getStartDate()));
            if (childEvent.getEndDate() != null) {
                if (childEvent.isAllDay()) {
                    result.setEnd(dateTimeFormatter.format(new Date(childEvent.getEndDate().getTime() + TimeUnit.DAYS.toMillis(1))));
                } else {
                    result.setEnd(dateTimeFormatter.format(childEvent.getEndDate()));
                }
            }

            result.setParticipants(parseParticipants(childEvent.getParticipants()));
        } else {
            DateTimeFormatter dateTimeFormatter = getDateFormatter(user, event.isAllDay());

            result.setTitle(event.getTitle());
            result.setOriginalId(String.valueOf(event.getID()));

            result.setAllDay(event.isAllDay());
            result.setStart(dateTimeFormatter.format(Date.from(startDate.toInstant())));
            if (endDate != null) {
                if (event.isAllDay()) {
                    result.setEnd(dateTimeFormatter.format(Date.from(endDate.plusDays(1).toInstant())));
                } else {
                    result.setEnd(dateTimeFormatter.format(Date.from(endDate.toInstant())));
                }
            }

            result.setParticipants(parseParticipants(event.getParticipants()));
        }
        result.setOriginalStart(event.getStartDate());
        result.setOriginalEnd(event.getEndDate());
        result.setOriginalAllDay(event.isAllDay());

        result.setId(event.getID() + "-" + number);
        result.setCalendarId(event.getCalendarId());
        result.setRecurrenceNumber(number);

        result.setColor(calendar.getColor());
        result.setType(EventDto.Type.CUSTOM);
        result.setIssueTypeImgUrl(event.getEventType().getAvatar());
        result.setRecurring(true);

        result.setStartEditable(canEditEvents);
        result.setDurationEditable(canEditEvents);

        return result;
    }

    private UserDto buildUser(ApplicationUser user) {
        UserDto result = new UserDto();
        result.setKey(user.getKey());
        result.setName(user.getName());
        result.setDisplayName(user.getDisplayName());
        result.setAvatarUrl(avatarService.getAvatarURL(user, user, Avatar.Size.SMALL).toString());
        return result;
    }

    private UserDto buildNonExistingUser(String key) {
        UserDto result = new UserDto();
        result.setKey(key);
        result.setName(key);
        result.setDisplayName(key);
        result.setAvatarUrl(null);
        return result;
    }

    private EventTypeDto buildEventType(EventType eventType, EventTypeReminder reminder) {
        EventTypeDto dto = new EventTypeDto();

        dto.setAvatar(eventType.getAvatar());
        dto.setId(eventType.getID());
        dto.setName(getTypeDisplayName(eventType));
        dto.setSystem(eventType.isSystem());

        if (reminder != null) {
            dto.setReminder(reminder.getReminderType().name());
        }

        return dto;
    }

    private DateTimeFormatter getDateFormatter(ApplicationUser user, boolean allDay) {
        if (allDay) {
            return jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE).withZone(UTC_TZ);
        } else {
            return jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);
        }
    }

    private boolean isBeforeEnd(ZonedDateTime time, ZonedDateTime endTime) {
        return endTime == null || time.isBefore(endTime);
    }

    private boolean isCountOk(int number, Integer recurrenceCount) {
        return recurrenceCount == null || number <= recurrenceCount;
    }

    private boolean isLimitExceeded(int currentEventCount) {
        if (currentEventCount > GENERATION_LIMIT_PER_REQUEST) {
            logger.warn("Recurrent event limit per request exceeded");
            return true;
        }
        return false;
    }

    private DateGenerator getDateGenerator(Event event, ZoneId zoneId) {
        ZonedDateTime startDate = event.getStartDate().toInstant().atZone(zoneId);
        switch (event.getRecurrenceType()) {
            case DAILY:
                return new ChronoUnitDateGenerator(
                    ChronoUnit.DAYS,
                    event.getRecurrencePeriod(),
                    startDate
                );
            case WEEKDAYS:
                return new DaysOfWeekDateGenerator(
                    ImmutableSet.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                    ),
                    event.getRecurrencePeriod(),
                    startDate
                );
            case MON_WED_FRI:
                return new DaysOfWeekDateGenerator(
                    ImmutableSet.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.FRIDAY
                    ),
                    event.getRecurrencePeriod(),
                    startDate
                );
            case TUE_THU:
                return new DaysOfWeekDateGenerator(
                    ImmutableSet.of(
                        DayOfWeek.TUESDAY,
                        DayOfWeek.THURSDAY
                    ),
                    event.getRecurrencePeriod(),
                    startDate
                );
            case DAYS_OF_WEEK:
                return new DaysOfWeekDateGenerator(
                    Arrays
                        .stream(event.getRecurrenceExpression().split(","))
                        .map(DayOfWeek::valueOf)
                        .collect(Collectors.toSet()),
                    event.getRecurrencePeriod(),
                    startDate
                );
            case MONTHLY:
                return new ChronoUnitDateGenerator(
                    ChronoUnit.MONTHS,
                    event.getRecurrencePeriod(),
                    startDate
                );
            case YEARLY:
                return new ChronoUnitDateGenerator(
                    ChronoUnit.YEARS,
                    event.getRecurrencePeriod(),
                    startDate
                );
            case CRON:
                return new CronDateGenerator(
                    event.getRecurrenceExpression(),
                    zoneId,
                    startDate
                );
        }
        return null;
    }
}
