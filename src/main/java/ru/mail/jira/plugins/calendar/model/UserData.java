package ru.mail.jira.plugins.calendar.model;

import net.java.ao.Entity;

/**
 * User preferences
 */
public interface UserData extends Entity {
    /** Userkey */
    String getUserKey();
    void setUserKey(String userKey);

    /** Hash for ical url */
    String getIcalUid();
    void setICalUid(String uid);

    /** Rating dialog statistic */
    long getNextFeedbackShow();
    void setNextFeedbackShow(long timestamp);
    int getFeedbackShowCount();
    void setFeedbackShowCount(int feedbackShowCount);


    /** Week, day or month */
    @Deprecated
    String getDefaultView();
    @Deprecated
    void setDefaultView(String defaultView);

    /** Hide weekends or not */
    @Deprecated
    boolean isHideWeekends();
    @Deprecated
    void setHideWeekends(boolean hideWeekends);

    /** Show time or not */
    @Deprecated
    boolean isShowTime();
    @Deprecated
    void setShowTime(boolean showTime);

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
}
