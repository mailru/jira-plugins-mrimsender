package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.sql.Timestamp;

@XmlRootElement
@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventMoveDto {
    @XmlElement
    private Timestamp start;
    @XmlElement
    private Timestamp end;
    @XmlElement
    private boolean allDay;
    @XmlElement
    private Integer parentId;
    @XmlElement
    private Integer recurrenceNumber;
    @XmlElement
    private EditMode editMode;
}
