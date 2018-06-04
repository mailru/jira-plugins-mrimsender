package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Getter @Setter @XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventTypeDto {
    @XmlElement
    private int id;
    @XmlElement
    private int calendarId;
    @XmlElement
    private boolean system;
    @XmlElement
    private String name;
    @XmlElement
    private String avatar;
    @XmlElement
    private String reminder;
}
