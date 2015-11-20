package ru.mail.jira.plugins.calendar.model.dto;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import ru.mail.jira.plugins.calendar.model.CalendarFeed;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created by i.mashintsev on 20.11.15.
 */
@XmlRootElement
public class CalendarFeedDto {
    @XmlElement
    private String feedUid;
    @XmlElement
    private String userKey;

    public CalendarFeedDto() {
    }

    public CalendarFeedDto(CalendarFeed feed) {
        userKey = feed.getUserKey();
        feedUid = feed.getUid();
    }

    public String getFeedUid() {
        return feedUid;
    }

    public void setFeedUid(String feedUid) {
        this.feedUid = feedUid;
    }

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }
}
