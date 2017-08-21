package ru.mail.jira.plugins.calendar.service.recurrent;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.sal.api.message.I18nResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.Event;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.commons.RestFieldException;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.ZonedDateTime;

public abstract class AbstractRecurrenceStrategy implements RecurrenceStrategy {
    private static final long LIMIT_PER_REQUEST = 1000;

    protected final Logger logger = LoggerFactory.getLogger(AbstractRecurrenceStrategy.class);

    @Override
    public final void validateDto(I18nResolver i18nResolver, CustomEventDto customEventDto) {
        Integer recurrenceCount = customEventDto.getRecurrenceCount();
        Timestamp recurrenceEndDate = customEventDto.getRecurrenceEndDate();

        if (recurrenceCount != null && recurrenceEndDate != null) {
            throw new IllegalArgumentException("count and end date cannot be present at same time"); //todo
        }

        if (recurrenceCount != null) {
            if (recurrenceCount <= 0) {
                throw new RestFieldException("Count should be >= 0", "recurrenceCount"); //todo
            }
        }

        if (recurrenceEndDate != null) {
            if (recurrenceEndDate.before(customEventDto.getStartDate())) {
                throw new RestFieldException("End date should be after start date", "recurrenceEndDate");
            }
        }

        validate(i18nResolver, customEventDto);
    }

    protected abstract void validate(I18nResolver i18nResolver, CustomEventDto customEventDto);

    /*
        Utility methods
     */

    protected EventDto buildEvent(Event event, int number, ZonedDateTime startDate, ZonedDateTime endDate, EventContext eventContext) {
        DateTimeFormatter dateTimeFormatter = eventContext.getDateTimeFormatter();

        EventDto result = new EventDto();
        result.setId(event.getID() + "-" + number);
        result.setOriginalId(String.valueOf(event.getID()));
        result.setCalendarId(event.getCalendarId());
        result.setTitle(event.getTitle());

        result.setColor(eventContext.getCalendar().getColor());
        result.setType(EventDto.Type.CUSTOM);
        result.setIssueTypeImgUrl(event.getEventType().getAvatar());
        result.setAllDay(event.isAllDay());
        result.setRecurring(true);
        result.setAllDay(event.isAllDay());
        result.setParticipants(eventContext.getParticipants());

        result.setStart(dateTimeFormatter.format(Date.from(startDate.toInstant())));

        if (endDate != null) {
            if (event.isAllDay()) {
                result.setEnd(dateTimeFormatter.format(Date.from(endDate.plusDays(1).toInstant())));
            } else {
                result.setEnd(dateTimeFormatter.format(Date.from(endDate.toInstant())));
            }
        }

        result.setStartEditable(eventContext.isCanEditEvents());
        result.setDurationEditable(eventContext.isCanEditEvents());

        return result;
        //todo
    }

    protected boolean isBeforeEnd(ZonedDateTime time, ZonedDateTime endTime) {
        return endTime == null || time.isBefore(endTime);
    }

    protected boolean isCountOk(int number, Integer recurrenceCount) {
        return recurrenceCount == null || number <= recurrenceCount;
    }

    protected void validatePeriod(I18nResolver i18nResolver, CustomEventDto eventDto) {
        if (eventDto.getRecurrencePeriod() == null) {
            throw new RestFieldException(i18nResolver.getText("issue.field.required", i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.period")), "recurrencePeriod");
        }

        if (eventDto.getRecurrencePeriod() <= 0) {
            throw new RestFieldException(i18nResolver.getText("ru.mail.jira.plugins.calendar.customEvents.dialog.error.incorrectPeriod"), "recurrencePeriod");
        }
    }

    protected boolean isLimitExceeded(int currentEventCount) {
        if (currentEventCount > LIMIT_PER_REQUEST) {
            logger.warn("Recurrent event limit per request exceeded");
            return true;
        }
        return false;
    }
}
