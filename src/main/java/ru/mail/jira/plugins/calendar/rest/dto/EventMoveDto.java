package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;

@XmlRootElement
@Getter @Setter
public class EventMoveDto {
    @XmlElement
    private Timestamp start;
    @XmlElement
    private Timestamp end;
    @XmlElement
    private boolean allDay;
}
