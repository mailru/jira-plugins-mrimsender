package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;

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
    boolean isShowTime();
    void setShowTime(boolean showTime);

    /** Hide weekends or not */
    boolean isHideWeekends();
    void setHideWeekends(boolean hideWeekends);

    /** Ids of showed calendars */
    String getShowedCalendars();
    void setShowedCalendars(String showedCalendars);

    /** Hash for ical url */
    String getIcalUid();
    void setICalUid(String uid);
}
