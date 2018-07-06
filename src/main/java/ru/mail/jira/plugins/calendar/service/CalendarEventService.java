package ru.mail.jira.plugins.calendar.service;

import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.Query;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.IssueInfo;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public interface CalendarEventService {
    String CREATED_DATE_KEY = "created";
    String UPDATED_DATE_KEY = "updated";
    String RESOLVED_DATE_KEY = "resolved";
    String DUE_DATE_KEY = "due_date";

    EventDto getEvent(int calendarId, ApplicationUser user, String issueKey, boolean forGantt) throws GetException;

    List<EventDto> findEvents(
        int calendarId, String groupBy,
        String start,
        String end,
        ApplicationUser user,
        boolean includeIssueInfo
    ) throws ParseException, SearchException, GetException;

    IssueInfo getEventInfo(ApplicationUser user, int calendarId, String eventId) throws GetException;

    Stream<EventDto> getGanttUnboundedEvents(
        Calendar calendar,
        String groupBy,
        ApplicationUser user,
        Order order,
        Long sprintId,
        List<String> fields,
        boolean forPlan
    ) throws SearchException;

    Stream<EventDto> getGanttEventsWithDuration(
        Calendar calendar,
        String groupBy,
        ApplicationUser user,
        Order order,
        Date startTime,
        Date endTime,
        List<String> fields
    ) throws SearchException;

    Query getUnboundedEventsQuery(ApplicationUser user, Calendar calendar, Long sprintId, Order order, boolean onlyEstimated, boolean onlyUnresolved);

    EventDto moveEvent(ApplicationUser user, int calendarId, String eventId, String start, String end, boolean forGantt) throws Exception;

    EventDto moveEvent(ApplicationUser user, int calendarId, String eventId, String start, String end, String estimate, boolean forGantt) throws Exception;

    EventDto moveEvent(ApplicationUser user, int calendarId, String eventId, String start, String end, String estimate, List<String> fields, boolean forGantt) throws Exception;

    List<EventDto> getHolidays(ApplicationUser user) throws GetException;
}
