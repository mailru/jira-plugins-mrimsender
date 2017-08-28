package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
@Getter
@Setter
public class CustomEventDto {
    @XmlElement
    private Integer id;
    @XmlElement
    private int calendarId;
    @XmlElement
    private String calendarName;
    @XmlElement
    private String title;
    @XmlElement
    private String originalTitle;
    @XmlElement
    private Timestamp startDate;
    @XmlElement
    private Timestamp endDate;
    @XmlElement
    private Timestamp originalStartDate;
    @XmlElement
    private Timestamp originalEndDate;
    @XmlElement
    private Boolean originalAllDay;
    @XmlElement
    private Integer parentId;
    @XmlElement
    private int eventTypeId;
    @XmlElement
    private String eventTypeName;
    @XmlElement
    private String eventTypeI18nName;
    @XmlElement
    private String eventTypeAvatar;
    @XmlElement
    private String participantNames;
    @XmlElement
    private boolean editable;
    @XmlElement
    private boolean allDay;
    @XmlElement
    private String reminder;
    @XmlElement
    private List<UserDto> participants;
    @XmlElement
    private String recurrenceType;
    @XmlElement
    private String recurrenceExpression;
    @XmlElement
    private Integer recurrencePeriod;
    @XmlElement
    private Timestamp recurrenceEndDate;
    @XmlElement
    private Integer recurrenceCount;
    @XmlElement
    private EditMode editMode;
    @XmlElement
    private Integer recurrenceNumber;
}
