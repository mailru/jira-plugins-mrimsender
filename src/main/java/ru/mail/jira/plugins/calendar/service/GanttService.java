package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.search.SearchException;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttLinkForm;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttTaskDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttLinkDto;

import java.text.ParseException;

@Transactional
public interface GanttService {
    GanttDto getGantt(int calendarId, String startDate, String endDate) throws ParseException, SearchException, GetException;

    GanttLinkDto createLink(int calendarId, GanttLinkForm form);
    void deleteLink(int calendarId, int linkId);

    GanttTaskDto updateDates(int calendarId, String issueKey, String startDate, String endDate) throws Exception;
}
