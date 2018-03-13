package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.query.order.SortOrder;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttLinkForm;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttTaskDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttLinkDto;

import java.text.ParseException;
import java.util.List;

@Transactional
public interface GanttService {
    GanttDto getGantt(ApplicationUser user, int calendarId, String startDate, String endDate, String groupBy, String orderBy, SortOrder order) throws ParseException, SearchException, GetException;

    GanttLinkDto createLink(ApplicationUser user, int calendarId, GanttLinkForm form) throws GetException;
    void deleteLink(ApplicationUser user, int calendarId, int linkId) throws GetException;

    List<GanttTaskDto> updateDates(ApplicationUser user, int calendarId, String issueKey, String startDate, String endDate) throws Exception;
}
