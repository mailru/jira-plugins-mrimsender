package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Setter
@Getter
@XmlRootElement
public class GanttResourceDto {
    @XmlElement
    private String id;
    @XmlElement
    private String text;
    @XmlElement
    private String parent;

    public GanttResourceDto(String id, String text, String parent) {
        this.id = id;
        this.text = text;
        this.parent = parent;
    }
}
