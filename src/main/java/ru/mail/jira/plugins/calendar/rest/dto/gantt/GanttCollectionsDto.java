package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Setter @Getter
@XmlRootElement
public class GanttCollectionsDto {
    @XmlElement
    private List<GanttLinkDto> links;
}
