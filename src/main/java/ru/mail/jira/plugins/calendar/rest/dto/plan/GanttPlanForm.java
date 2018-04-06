package ru.mail.jira.plugins.calendar.rest.dto.plan;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Getter @Setter
@XmlRootElement
public class GanttPlanForm {
    @XmlElement
    private List<GanttPlanItem> items;
}
