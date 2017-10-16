package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement @Getter @Setter @Builder
public class EventGroup {
    @XmlElement
    private String id;
    @XmlElement
    private String name;
    @XmlElement
    private String avatar;
}
