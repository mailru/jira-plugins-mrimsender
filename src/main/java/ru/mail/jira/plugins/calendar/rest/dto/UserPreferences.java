package ru.mail.jira.plugins.calendar.rest.dto;

import ru.mail.jira.plugins.calendar.model.UserData;

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
    private String icalUid;
    @XmlElement
    private Long lastLikeFlagShown;

    public UserPreferences() {
    }

    public UserPreferences(UserData userData) {
        if (userData != null) {
            this.calendarView = userData.getDefaultView();
            this.hideWeekends = userData.isHideWeekends();
            this.showTimeInDates = userData.isShowTime();
            this.icalUid = userData.getIcalUid();
        }
    }

    public void setLastLikeFlagShown(Long lastLikeFlagShown) {
        this.lastLikeFlagShown = lastLikeFlagShown;
    }
}
