package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@Getter @Setter
@ToString
public class GanttTaskDto extends GanttTaskForm {
    @XmlElement
    private String id;
    @XmlElement
    private String summary;
    @XmlElement
    private String text;
    @XmlElement(name = "icon_src")
    private String iconSrc;
    @XmlElement
    private String assignee;
    @XmlElement
    private double progress;
    @XmlElement
    private String estimate;
    @XmlElement
    private String parent;
    @XmlElement
    private boolean movable;
    @XmlElement
    private boolean resizable;
    @XmlElement
    private boolean resolved;
    @XmlElement
    private Long overdueSeconds;
    @XmlElement
    private Long earlySeconds;
    private EventDto originalEvent;
}
