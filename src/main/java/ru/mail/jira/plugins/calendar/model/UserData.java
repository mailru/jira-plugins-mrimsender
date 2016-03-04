package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;
import net.java.ao.ManyToMany;
import net.java.ao.OneToMany;

/**
 * User preferences
 */
public interface UserData extends Entity {
    /** Userkey */
    String getUserKey();
    void setUserKey(String userKey);

    /** Week, day or month */
    String getDefaultView();
    void setDefaultView(String defaultView);

    /** Show time or not */
    @Deprecated
    boolean isShowTime();
    @Deprecated
    void setShowTime(boolean showTime);

    /** Hide weekends or not */
    boolean isHideWeekends();
    void setHideWeekends(boolean hideWeekends);

    /** Ids of showed calendars */
    @Deprecated
    String getShowedCalendars();
    @Deprecated
    void setShowedCalendars(String showedCalendars);

    /** Ids of favorite calendars */
    @Deprecated
    String getFavoriteCalendars();
    @Deprecated
    void setFavoriteCalendars(String favoriteCalendars);

    /** Hash for ical url */
    String getIcalUid();
    void setICalUid(String uid);

    /** Last like flag shown */
    long getLastLikeFlagShown();
    void setLastLikeFlagShown(long timestamp);
}
