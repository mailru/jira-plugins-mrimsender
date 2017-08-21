package ru.mail.jira.plugins.calendar.service.recurrent;

import com.atlassian.jira.datetime.DateTimeFormatter;
import lombok.Getter;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.rest.dto.UserDto;

import java.time.ZoneId;
import java.util.List;

@Getter
public class EventContext {
    private final Calendar calendar;
    private final DateTimeFormatter dateTimeFormatter;
    private final boolean canEditEvents;
    private final List<UserDto> participants;
    private final ZoneId zoneId;

    public EventContext(Calendar calendar, DateTimeFormatter dateTimeFormatter, boolean canEditEvents, List<UserDto> participants, ZoneId zoneId) {
        this.calendar = calendar;
        this.dateTimeFormatter = dateTimeFormatter;
        this.canEditEvents = canEditEvents;
        this.participants = participants;
        this.zoneId = zoneId;
    }
}
