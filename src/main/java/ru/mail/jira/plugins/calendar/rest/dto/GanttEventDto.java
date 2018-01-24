package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
@Getter
@Setter
@ToString
public class GanttEventDto {
    @XmlElement
    private String id;
    @XmlElement
    private String text;
    @XmlElement(name = "start_date")
    private String startDate;
    @XmlElement(name = "end_date")
    private String endDate;
    @XmlElement
    private String duration;
    @XmlElement
    private float progress;
    @XmlElement
    private String parent;
}
