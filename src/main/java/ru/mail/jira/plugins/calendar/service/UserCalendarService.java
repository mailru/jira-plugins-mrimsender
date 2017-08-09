package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.exception.UpdateException;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.QuickFilter;
import ru.mail.jira.plugins.calendar.model.UserCalendar;

import java.util.List;

@Transactional
public interface UserCalendarService {
    UserCalendar[] find(String userKey);

    UserCalendar find(int calendarId, String userKey);

    void updateCalendarVisibility(int calendarId, String userKey, boolean visible) throws GetException;

    List<QuickFilter> getFavouriteQuickFilters(int calendarId, String userKey);

    void addToFavouriteQuickFilter(int calendarId, String userKey, int id, boolean addToFavourite) throws GetException, UpdateException;

    void selectQuickFilter(int calendarId, String userKey, int id, boolean selected) throws Exception;

    void addCalendarToUser(String userKey, Calendar calendar, boolean visible);

    UserCalendar get(int calendarId, String userKey) throws GetException;

    UserCalendar getOrCreate(Calendar calendar, String userKey);

    void removeCalendar(String userKey, Integer calendarId);

    int getUsersCount(final int calendarId);
}
