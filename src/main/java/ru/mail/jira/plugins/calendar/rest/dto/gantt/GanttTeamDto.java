package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.calendar.model.gantt.GanttTeam;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Setter
@Getter
@XmlRootElement
public class GanttTeamDto {
    @XmlElement
    private int id;
    @XmlElement
    private String name;
    @XmlElement
    private int calendarId;
    @XmlElement
    private List<GanttUserDto> users;

    public GanttTeamDto() {
    }

    public GanttTeamDto(GanttTeam team) {
        this.id = team.getID();
        this.name = team.getName();
        this.calendarId = team.getCalendarId();
    }
}
