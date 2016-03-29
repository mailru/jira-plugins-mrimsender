package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.exception.GetException;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.UserCalendar;

public class UserCalendarServiceImpl implements UserCalendarService {
    private ActiveObjects ao;

    public void setAo(ActiveObjects ao) {
        this.ao = ao;
    }

    @Override
    public UserCalendar[] find(String userKey) {
        return ao.find(UserCalendar.class, Query.select().where("USER_KEY = ?", userKey));
    }

    @Override
    public UserCalendar find(final int calendarId, final String userKey) {
        try {
            return get(calendarId, userKey);
        } catch (GetException e) {
            return null;
        }
    }

    @Override
    public void updateCalendarVisibility(final int calendarId, final String userKey, final boolean visible) throws GetException {
        UserCalendar userCalendar = get(calendarId, userKey);
        userCalendar.setEnabled(visible);
        userCalendar.save();
    }

    @Override
    public void addCalendarToUser(final String userKey, final Calendar calendar, final boolean visible) {
        UserCalendar userCalendar = getOrCreate(calendar, userKey);
        userCalendar.setEnabled(visible);
        userCalendar.save();
    }

    @Override
    public UserCalendar get(final int calendarId, final String userKey) throws GetException {
        UserCalendar[] userCalendars = ao.find(UserCalendar.class, Query.select().where("USER_KEY = ? AND CALENDAR_ID = ?", userKey, calendarId));
        if (userCalendars.length > 1)
            throw new ActiveObjectsException(String.format("Found more that one object of type UserCalendar for userData '%s' and calendar '%s'", userKey, calendarId));
        else if (userCalendars.length == 0)
            throw new GetException(String.format("User '%s' doesn't have calendar '%s'", userKey, calendarId));
        else
            return userCalendars[0];
    }

    @Override
    public UserCalendar getOrCreate(final Calendar calendar, final String userKey) {
        UserCalendar[] userCalendars = ao.find(UserCalendar.class, Query.select().where("USER_KEY = ? AND CALENDAR_ID = ?", userKey, calendar.getID()));
        if (userCalendars.length > 1)
            throw new ActiveObjectsException(String.format("Found more that one object of type UserCalendar for userData '%s' and calendar '%s'", userKey, calendar.getID()));
        else if (userCalendars.length == 0) {
            UserCalendar userCalendar = ao.create(UserCalendar.class);
            userCalendar.setCalendarId(calendar.getID());
            userCalendar.setColor(calendar.getColor());
            userCalendar.setName(calendar.getName());
            userCalendar.setUserKey(userKey);
            userCalendar.save();
            return userCalendar;
        } else
            return userCalendars[0];
    }

    @Override
    public void removeCalendar(String userKey, Integer calendarId) {
        ao.delete(ao.find(UserCalendar.class, Query.select().where("USER_KEY= ? AND CALENDAR_ID = ?", userKey, calendarId)));
    }

    public int getUsersCount(final int calendarId) {
        return ao.find(UserCalendar.class, Query.select().where("CALENDAR_ID = ? ", calendarId)).length;
    }
}
