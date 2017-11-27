package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.user.ApplicationUser;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.rest.dto.CustomEventDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventTypeDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventMoveDto;

import java.util.Date;
import java.util.List;

@Transactional
public interface CustomEventService {
    EventDto createEvent(ApplicationUser user, CustomEventDto eventDto) throws GetException;

    EventDto editEvent(ApplicationUser user, CustomEventDto eventDto) throws GetException;

    void deleteEvent(ApplicationUser user, int eventId) throws GetException;

    EventDto moveEvent(ApplicationUser user, int id, EventMoveDto moveDto) throws GetException;

    int getEventCount(ApplicationUser user, int calendarId) throws GetException;

    CustomEventDto getEventDto(ApplicationUser user, int id) throws GetException;

    List<EventDto> getEvents(ApplicationUser user, Calendar calendar, Date start, Date end, Date startUtc, Date endUtc);

    List<EventTypeDto> getTypes(ApplicationUser user, int calendarId) throws GetException;

    EventTypeDto createEventType(ApplicationUser user, EventTypeDto typeDto) throws GetException;

    EventTypeDto editEventType(ApplicationUser user, EventTypeDto typeDto) throws GetException;

    void deleteEventType(ApplicationUser user, int id) throws GetException;
}
