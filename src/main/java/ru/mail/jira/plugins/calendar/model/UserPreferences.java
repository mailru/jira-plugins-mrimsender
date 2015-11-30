package ru.mail.jira.plugins.calendar.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class UserPreferences {
    @XmlElement
    private String calendarView;
    @XmlElement
    private boolean showTimeInDates;
    @XmlElement
    private boolean hideWeekends;
    @XmlElement
    private String icalHashUrl;
    @XmlElement
    private String userKey;

    public UserPreferences() { }

    public UserPreferences(UserData userData) {
        if (userData != null) {
            this.calendarView = userData.getDefaultView();
            this.hideWeekends = userData.isHideWeekends();
            this.showTimeInDates = userData.isShowTime();
            this.icalHashUrl = userData.getIcalUid();
            this.userKey = userData.getUserKey();
        }
    }
}
