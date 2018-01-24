package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.search.SearchException;
import ru.mail.jira.plugins.calendar.model.GanttLink;
import ru.mail.jira.plugins.calendar.rest.dto.GanttDto;

import java.text.ParseException;

@Transactional
public interface GanttService {
    GanttDto getGantt(int calendarId) throws ParseException, SearchException, GetException;
    GanttLink createLink(int calendarId, String sourceKey, String targetKey, String type);
}
