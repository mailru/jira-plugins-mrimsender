package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Setter
@Getter
@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class GanttLinkDto {
    @XmlElement
    private int id;
    @XmlElement
    private String source;
    @XmlElement
    private String target;
    @XmlElement
    private String type;
}
