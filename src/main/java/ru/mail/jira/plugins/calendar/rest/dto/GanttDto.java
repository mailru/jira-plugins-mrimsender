package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.UserCalendar;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Setter
@Getter
@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class GanttDto {
    @XmlElement
    private GanttEventDto[] data;
    @XmlElement
    private GanttCollectionsDto collections;


    public GanttDto() {
    }

}
