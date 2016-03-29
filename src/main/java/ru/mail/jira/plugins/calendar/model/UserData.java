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

    /** Hide weekends or not */
    boolean isHideWeekends();
    void setHideWeekends(boolean hideWeekends);

    /** Hash for ical url */
    String getIcalUid();
    void setICalUid(String uid);

    /** Rating dialog statistic */
    long getLastLikeFlagShown();
    void setLastLikeFlagShown(long timestamp);
    void setPluginRated(boolean rated);
    boolean isPluginRated();
    int getLikeShowCount();
    void setLikeShowCount(int likeShowCount);

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
