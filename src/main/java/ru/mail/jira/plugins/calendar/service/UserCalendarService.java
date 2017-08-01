package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.exception.GetException;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.UserCalendar;

import java.util.Collection;

@Transactional
public interface UserCalendarService {
    UserCalendar[] find(String userKey);

    UserCalendar find(int calendarId, String userKey);

    void updateCalendarVisibility(int calendarId, String userKey, boolean visible) throws GetException;

    void addCalendarToUser(String userKey, Calendar calendar, boolean visible);

    UserCalendar get(int calendarId, String userKey) throws GetException;

    UserCalendar getOrCreate(Calendar calendar, String userKey);

    void removeCalendar(String userKey, Integer calendarId);

    int getUsersCount(final int calendarId);

    Collection<String> getEnabledUsersKeys(final int calendarId);
}
