package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Setter
@Getter
@XmlRootElement
public class GanttUserDto {
    @XmlElement
    private int id;
    @XmlElement
    private String key;
    @XmlElement
    private String displayName;
    @XmlElement
    private String avatarUrl;
    @XmlElement
    private String weeklyHours;

    public GanttUserDto() {
    }
}
