package ru.mail.jira.plugins.calendar.rest.dto;


import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
@Getter
@Setter
public class EventDto {
    @XmlElement
    private String id;
    @XmlElement
    private Type type;
    @XmlElement
    private String issueKey;
    @XmlElement
    private String issueTypeImgUrl;
    @XmlElement
    private String status;
    @XmlElement
    private String statusColor;
    @XmlElement
    private long calendarId;
    @XmlElement
    private boolean allDay;
    @XmlElement
    private String color;
    @XmlElement
    private String end;
    @XmlElement
    private String start;
    @XmlElement
    private String title;
    @XmlElement
    private boolean durationEditable;
    @XmlElement
    private boolean startEditable;
    @XmlElement
    private boolean datesError;
    @XmlElement
    private IssueInfo issueInfo;
    @XmlElement
    private List<UserDto> participants;

    public enum Type {
        ISSUE,
        CUSTOM
    }
}
