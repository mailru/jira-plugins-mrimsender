package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.user.ApplicationUser;
import ru.mail.jira.plugins.calendar.model.QuickFilter;

@Transactional
public interface QuickFilterService {
    QuickFilter getQuickFilter(int id) throws GetException;

    QuickFilter findQuickFilter(int id);

    QuickFilter createQuickFilter(int calendarId, String name, String jql, String description, Boolean share, ApplicationUser user) throws Exception;

    QuickFilter updateQuickFilter(int id, int calendarId, String name, String jql, String description, Boolean share, ApplicationUser user) throws Exception;

    void deleteQuickFilterById(int id, ApplicationUser user) throws GetException;

    void deleteQuickFilterByCalendarId(int calendarId) throws GetException;

    QuickFilter[] getQuickFilters(int calendarId, ApplicationUser user);
}
