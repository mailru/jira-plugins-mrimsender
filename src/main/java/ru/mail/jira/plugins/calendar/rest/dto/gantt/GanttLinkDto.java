package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Getter @Setter
@XmlRootElement
public class GanttLinkDto extends GanttLinkForm {
    @XmlElement
    private int id;
    @XmlElement
    private String color;
}
