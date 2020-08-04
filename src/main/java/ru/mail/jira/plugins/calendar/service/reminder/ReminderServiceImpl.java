package ru.mail.jira.plugins.calendar.service.reminder;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.mail.builder.EmailBuilder;
import com.atlassian.jira.notification.NotificationRecipient;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.mail.queue.MailQueue;
import com.atlassian.mail.queue.MailQueueItem;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.common.Consts;
import ru.mail.jira.plugins.calendar.model.*;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.calendar.rest.dto.UserDto;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.calendar.service.PluginData;
import ru.mail.jira.plugins.calendar.service.UserCalendarService;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReminderServiceImpl implements ReminderService {
    private final Logger logger = LoggerFactory.getLogger(ReminderServiceImpl.class);

    private final ActiveObjects ao;
    private final UserManager userManager;
    private final MailQueue mailQueue;
    private final AvatarService avatarService;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final TimeZoneManager timeZoneManager;
    private final DateTimeFormatter dateTimeFormatter;
    private final UserCalendarService userCalendarService;
    private final CalendarService calendarService;
    private final PluginData pluginData;
    private final EventPublisher eventPublisher;

    @Autowired
    public ReminderServiceImpl(
            @ComponentImport UserManager userManager,
            @ComponentImport MailQueue mailQueue,
            @ComponentImport AvatarService avatarService,
            @ComponentImport I18nResolver i18nResolver,
            @ComponentImport LocaleManager localeManager,
            @ComponentImport TimeZoneManager timeZoneManager,
            @ComponentImport ActiveObjects ao,
            @ComponentImport DateTimeFormatter dateTimeFormatter,
            UserCalendarService userCalendarService,
            CalendarService calendarService,
            PluginData pluginData,
            @ComponentImport EventPublisher eventPublisher) {
        this.ao = ao;
        this.userManager = userManager;
        this.mailQueue = mailQueue;
        this.avatarService = avatarService;
        this.localeManager = localeManager;
        this.i18nResolver = i18nResolver;
        this.timeZoneManager = timeZoneManager;
        this.dateTimeFormatter = dateTimeFormatter;
        this.userCalendarService = userCalendarService;
        this.calendarService = calendarService;
        this.pluginData = pluginData;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void triggerNotificationsInRange(long since, long until) {
        logger.debug("running reminders");
        ListMultimap<Integer, CustomEventDto> events = ArrayListMultimap.create();

        for (ReminderType reminderType : ReminderType.values()) {
            long correctedSince = since + reminderType.getDurationMillis();
            long correctedUntil = until + reminderType.getDurationMillis();

            EventTypeReminder[] reminders = ao.find(
                EventTypeReminder.class,
                Query.select()
                    .alias(EventTypeReminder.class, "REMINDER")
                    .where("REMINDER_TYPE = ?", reminderType.name())
            );

            for (EventTypeReminder reminder : reminders) {
                EventType eventType = reminder.getEventType();

                TimeZone systemTimeZone = timeZoneManager.getDefaultTimezone();

                Event[] foundEvents = ao.find(
                    Event.class,
                    Query
                        .select()
                        .alias(Event.class, "EVENT")
                        .where(
                            "(EVENT.START_DATE >= ? AND EVENT.START_DATE < ? AND EVENT.ALL_DAY = ? OR EVENT.START_DATE >= ? AND EVENT.START_DATE < ? AND EVENT.ALL_DAY = ?) AND EVENT.EVENT_TYPE_ID = ? AND EVENT.CALENDAR_ID = ?",
                            new Timestamp(correctedSince),
                            new Timestamp(correctedUntil),
                            Boolean.FALSE,
                            new Timestamp(correctedSince + systemTimeZone.getOffset(correctedSince)),
                            new Timestamp(correctedUntil + systemTimeZone.getOffset(correctedUntil)),
                            Boolean.TRUE,
                            eventType.getID(),
                            reminder.getCalendarId()
                        )
                );

                for (Event event : foundEvents) {
                    events.put(event.getCalendarId(), buildEvent(event));
                }
            }
        }

        for (Integer calendarId : events.keySet()) {
            try {
                Calendar calendar = calendarService.getCalendar(calendarId);

                Set<ApplicationUser> usersKeys = userCalendarService
                    .getEnabledUsersKeys(calendarId)
                    .stream()
                    .map(userManager::getUserByKey)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

                for (CustomEventDto event : events.get(calendarId)) {
                    event.setCalendarId(calendar.getID());
                    event.setCalendarName(calendar.getName());

                    sendEventNotification(event, usersKeys);
                }
            } catch (GetException e) {
                logger.warn("unable to get calendar while processing reminders", e);
            }
        }
        logger.debug("reminders completed");
    }

    private CustomEventDto buildEvent(Event event) {
        CustomEventDto dto = new CustomEventDto();
        EventType eventType = event.getEventType();

        dto.setId(event.getID());
        dto.setTitle(event.getTitle());
        dto.setEndDate(event.getEndDate());
        dto.setStartDate(event.getStartDate());
        dto.setEventTypeId(eventType.getID());
        dto.setEventTypeName(eventType.getName());
        dto.setEventTypeI18nName(eventType.getI18nName());
        dto.setEventTypeAvatar(eventType.getAvatar());
        dto.setAllDay(event.isAllDay());

        List<UserDto> participants = parseParticipants(event.getParticipants());
        dto.setParticipants(participants);
        if (participants != null) {
            dto.setParticipantNames(participants.stream().map(UserDto::getName).collect(Collectors.joining(", ")));
        }
        return dto;
    }

    private void sendEventNotification(CustomEventDto event, Set<ApplicationUser> recipients) {
        logger.debug("Sending notification for event \"{}\" ({}) to users {}", event.getTitle(), event.getId(), recipients);
        for (ApplicationUser recipient : recipients) {
            boolean isHtmlFormat = false;

            DateTimeFormatter dateFormatter;
            if (event.isAllDay()) {
                dateFormatter = dateTimeFormatter.forUser(recipient).withStyle(DateTimeStyle.DATE_PICKER).withZone(Consts.UTC_TZ);
            } else {
                dateFormatter = dateTimeFormatter.forUser(recipient).withStyle(DateTimeStyle.DATE_TIME_PICKER);
            }

            Locale userLocale = localeManager.getLocaleFor(recipient);
            String subject = i18nResolver.getText(userLocale, "ru.mail.jira.plugins.calendar.customEvents.reminder.emailSubject", event.getTitle());

            Map<String, Object> params = new HashMap<>();
            params.put("event", event);
            params.put("pageTitle", subject);
            params.put("date", dateFormatter);

            MailQueueItem email = new EmailBuilder(new Email(recipient.getEmailAddress()), new NotificationRecipient(recipient))
                .withSubject(subject)
                .withBodyFromFile(isHtmlFormat ? "ru/mail/jira/plugins/calendar/reminder/reminder-html.vm" : "ru/mail/jira/plugins/calendar/reminder/reminder-html.vm")
                .addParameters(params)
                .renderLater();

            mailQueue.addItem(email);
            if(pluginData.getEventIdForRemind() != 0){
                eventPublisher.publish(new IssueEvent(null, params, recipient, pluginData.getEventIdForRemind(), false));
            }
        }
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
                UserDto userDto = new UserDto();
                userDto.setKey(participant.getKey());
                userDto.setName(participant.getName());
                userDto.setDisplayName(participant.getDisplayName());
                userDto.setAvatarUrl(avatarService.getAvatarURL(participant, participant, Avatar.Size.SMALL).toString());
                result.add(userDto);
            }
        }

        return result;
    }
}
