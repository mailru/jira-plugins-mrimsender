package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.calendar.rest.dto.UserDto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Setter
@Getter
@XmlRootElement
public class GanttUserDto extends UserDto {
    @XmlElement
    private int id;
    @XmlElement
    private String weeklyHours;

    public GanttUserDto() {
    }
}
