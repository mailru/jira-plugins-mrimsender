package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.ImmutableSet;
import net.java.ao.DBParam;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.model.*;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.rest.dto.*;
import ru.mail.jira.plugins.commons.RestFieldException;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class CustomEventServiceImpl implements CustomEventService {
    private static final Set<String> SUPPORTED_AVATAR_NAMES = ImmutableSet.of("event", "travel", "birthday", "leave");
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    private final ActiveObjects ao;
    private final I18nResolver i18nResolver;
    private final JiraDeprecatedService jiraDeprecatedService;
    private final UserManager userManager;
    private final AvatarService avatarService;
    private final CalendarService calendarService;
    private final PermissionService permissionService;

    @Autowired
    public CustomEventServiceImpl(
        @ComponentImport I18nResolver i18nResolver,
        @ComponentImport UserManager userManager,
        @ComponentImport AvatarService avatarService,
        @ComponentImport ActiveObjects ao,
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
    }

    @Override
    public EventDto createEvent(ApplicationUser user, CustomEventDto eventDto) throws GetException {
        preValidate(user, eventDto);

        EventType eventType = getEventType(eventDto.getEventTypeId());
        Calendar calendar = calendarService.getCalendar(eventDto.getCalendarId());

        if (!permissionService.hasEditEventsPermission(user, calendar)) {
            throw new SecurityException("No permission to create events");
        }

        if (eventType.isDeleted()) {
            throw new IllegalArgumentException(i18nResolver.getText("ru.mail.jira.plguins.calendar.customEvents.eventTypeIsDeleted"));
        }

        validateAndNormalize(eventDto, calendar, eventType);

        Event customEvent = ao.create(
            Event.class,
            new DBParam("TITLE", eventDto.getTitle()),
            new DBParam("CALENDAR_ID", eventDto.getCalendarId()),
            new DBParam("START_DATE", eventDto.getStartDate()),
            new DBParam("END_DATE", eventDto.getEndDate()),
            new DBParam("EVENT_TYPE_ID", eventType.getID()),
            new DBParam("CREATOR_KEY", user.getKey()),
            new DBParam("PARTICIPANTS", eventDto.getParticipantNames()),
            new DBParam("ALL_DAY", eventDto.isAllDay())
        );

        return buildEvent(user, customEvent, calendar, true);
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

        validateAndNormalize(eventDto, calendar, eventType);

        //keep type if deleted, but not modified
        if (eventType.isDeleted() && eventDto.getEventTypeId() != event.getEventType().getID()) {
            throw new IllegalArgumentException(i18nResolver.getText("ru.mail.jira.plguins.calendar.customEvents.eventTypeIsDeleted"));
        }

        event.setStartDate(eventDto.getStartDate());
        event.setEndDate(eventDto.getEndDate());
        event.setTitle(eventDto.getTitle());
        event.setEventType(eventType);
        event.setParticipants(eventDto.getParticipantNames());
        event.setAllDay(eventDto.isAllDay());
        event.save();

        return buildEvent(user, event, calendar, true);
    }

    @Override
    public void deleteEvent(ApplicationUser user, int eventId) throws GetException {
        Event event = getEvent(eventId);
        Calendar calendar = calendarService.getCalendar(event.getCalendarId());

        if (!permissionService.hasEditEventsPermission(user, calendar)) {
            throw new SecurityException("No permission to edit event");
        }

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

        event.setAllDay(moveDto.isAllDay());
        event.setStartDate(moveDto.getStart());
        event.setEndDate(moveDto.getEnd());
        event.save();

        return buildEvent(user, event, calendar, true);
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

        Calendar calendar = calendarService.getCalendar(event.getCalendarId());

        boolean hasEditPermission = permissionService.hasEditEventsPermission(user, calendar);
        if (!permissionService.hasUsePermission(user, calendar) && !hasEditPermission) {
            throw new SecurityException("No permission to view event");
        }

        CustomEventDto result = new CustomEventDto();
        EventType eventType = event.getEventType();

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

    private void validateAndNormalize(CustomEventDto eventDto, Calendar calendar, EventType eventType) {
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
                    "(START_DATE >= ? AND (END_DATE <= ? OR END_DATE IS NULL AND START_DATE <= ?) OR (START_DATE <= ? AND END_DATE >= ?) OR (START_DATE <= ? AND END_DATE >= ?)) AND CALENDAR_ID = ?",
                    start, end, end, start, start, end, end, calendar.getID()
                )
        );
        boolean canEditEvents = permissionService.hasEditEventsPermission(user, calendar);
        for (Event customEvent : customEvents) {
            result.add(buildEvent(user, customEvent, calendar, canEditEvents));
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
        DateTimeFormatter dateFormatter;
        if (event.isAllDay()) {
            dateFormatter = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE).withZone(UTC_TZ);
        } else {
            dateFormatter = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);
        }

        EventDto result = new EventDto();
        result.setCalendarId(event.getCalendarId());
        result.setId(String.valueOf(-1 * event.getID()));
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
}
