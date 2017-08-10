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
    private Timestamp startDate;
    @XmlElement
    private Timestamp endDate;
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
}
