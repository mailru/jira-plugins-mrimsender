package ru.mail.jira.plugins.calendar.rest.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDto {
    @XmlElement
    private String id;
    @XmlElement
    private Type type;
    @XmlElement
    private EventTypeDto eventType;
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
    //for accessing between modules in back-end
    private Date endDate;
    private Date startDate;
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
    @XmlElement
    private boolean recurring;
    @XmlElement
    private Integer recurrenceNumber;
    @XmlElement
    private String rendering;
    @XmlElement
    private String originalId;
    @XmlElement
    private String parentId;
    @XmlElement
    private Timestamp originalStart;
    @XmlElement
    private Timestamp originalEnd;
    @XmlElement
    private Boolean originalAllDay;
    @XmlElement
    private String groupField;
    @XmlElement
    private List<EventGroup> groups;
    @XmlElement
    private String originalEstimate;
    @XmlElement
    private String timeSpent;
    @XmlElement
    private Long originalEstimateSeconds;
    @XmlElement
    private Long timeSpentSeconds;
    @XmlElement
    private UserDto assignee;
    private List<EventMilestoneDto> milestones;
    private boolean resolved;

    public enum Type {
        ISSUE,
        CUSTOM,
        HOLIDAY
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventDto eventDto = (EventDto) o;
        return Objects.equals(id, eventDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
