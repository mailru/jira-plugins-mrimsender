package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Setter
@Getter
@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class GanttDto {
    @XmlElement
    private GanttTaskDto[] data;
    @XmlElement
    private GanttCollectionsDto collections;


    public GanttDto() {
    }

}
