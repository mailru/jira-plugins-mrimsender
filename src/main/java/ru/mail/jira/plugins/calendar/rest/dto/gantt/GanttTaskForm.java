package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Getter @Setter
@XmlRootElement
public class GanttTaskForm {
    @XmlElement(name = "start_date")
    private String startDate;
    @XmlElement(name = "end_date")
    private String endDate;
}
