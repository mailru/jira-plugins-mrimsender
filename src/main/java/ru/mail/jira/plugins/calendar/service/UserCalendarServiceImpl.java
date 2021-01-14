package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.exception.UpdateException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.FavouriteQuickFilter;
import ru.mail.jira.plugins.calendar.model.QuickFilter;
import ru.mail.jira.plugins.calendar.model.UserCalendar;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class UserCalendarServiceImpl implements UserCalendarService {
    private final ActiveObjects ao;

    @Autowired
    public UserCalendarServiceImpl(@ComponentImport ActiveObjects ao) {
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
    public List<QuickFilter> getFavouriteQuickFilters(int calendarId, String userKey) {
        UserCalendar userCalendar = find(calendarId, userKey);
        if (userCalendar == null)
            return null;
        List<QuickFilter> favouriteQuickFilters = new ArrayList<QuickFilter>();
        for (FavouriteQuickFilter favouriteQuickFilter : userCalendar.getFavouriteQuickFilters())
            favouriteQuickFilters.add(favouriteQuickFilter.getQuickFilter());
        return favouriteQuickFilters;
    }

    @Override
    public void addToFavouriteQuickFilter(int calendarId, String userKey, int id, boolean addToFavourite) throws GetException, UpdateException {
        UserCalendar userCalendar = get(calendarId, userKey);
        if (addToFavourite) {
            FavouriteQuickFilter[] favouriteQuickFilters = ao.find(FavouriteQuickFilter.class, Query.select().where("QUICK_FILTER_ID = ? AND USER_CALENDAR_ID = ?", id, userCalendar.getID()));
            if (favouriteQuickFilters.length != 0)
                throw new UpdateException("Filter is already add to favourite");
            FavouriteQuickFilter favouriteQuickFilter = ao.create(FavouriteQuickFilter.class);
            favouriteQuickFilter.setQuickFilter(ao.get(QuickFilter.class, id));
            favouriteQuickFilter.setUserCalendar(userCalendar);
            favouriteQuickFilter.setSelected(false);
            favouriteQuickFilter.save();
        } else
            ao.delete(ao.find(FavouriteQuickFilter.class, Query.select().where("QUICK_FILTER_ID = ? AND USER_CALENDAR_ID = ?", id, userCalendar.getID())));
    }

    @Override
    public void selectQuickFilter(int calendarId, String userKey, int id, boolean selected) throws Exception {
        UserCalendar userCalendar = get(calendarId, userKey);
        FavouriteQuickFilter[] favouriteQuickFilters = ao.find(FavouriteQuickFilter.class, Query.select().where("QUICK_FILTER_ID = ? AND USER_CALENDAR_ID = ?", id, userCalendar.getID()));
        if (favouriteQuickFilters.length == 1) {
            favouriteQuickFilters[0].setSelected(selected);
            favouriteQuickFilters[0].save();
        }
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
            userCalendar.setEditableSetting(calendar.getEditableSetting());
            userCalendar.setName(calendar.getName());
            userCalendar.setUserKey(userKey);
            userCalendar.save();
            return userCalendar;
        } else
            return userCalendars[0];
    }

    @Override
    public void removeCalendar(String userKey, Integer calendarId) {
        ao.delete(ao.find(FavouriteQuickFilter.class, Query.select().where("USER_CALENDAR_ID = ?", find(calendarId, userKey).getID())));
        ao.delete(ao.find(UserCalendar.class, Query.select().where("USER_KEY= ? AND CALENDAR_ID = ?", userKey, calendarId)));
    }

    public int getUsersCount(final int calendarId) {
        return ao.find(UserCalendar.class, Query.select().where("CALENDAR_ID = ? ", calendarId)).length;
    }

    @Override
    public Collection<String> getEnabledUsersKeys(final int calendarId) {
        return Arrays
            .stream(ao.find(UserCalendar.class, Query.select().where("CALENDAR_ID = ? AND ENABLED = ?", calendarId, Boolean.TRUE)))
            .map(UserCalendar::getUserKey)
            .collect(Collectors.toSet());
    }
}
