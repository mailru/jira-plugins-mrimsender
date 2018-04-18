package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;
import java.util.Objects;


@XmlRootElement
@Getter @Setter
@ToString
public class GanttTaskDto extends GanttTaskForm {
    @XmlElement
    private String id;
    @XmlElement
    private String entityId;
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
    private Long estimateSeconds;
    @XmlElement
    private String parent;
    @XmlElement
    private boolean movable;
    @XmlElement
    private boolean resizable;
    @XmlElement
    private boolean linkable;
    @XmlElement
    private boolean resolved;
    @XmlElement
    private Long overdueSeconds;
    @XmlElement
    private Long plannedDuration;
    @XmlElement
    private String type;
    @XmlElement
    private String resource;
    @XmlElement
    private Map<String, String> fields;
    @XmlElement
    private Long earlyDuration;
    @XmlElement
    private Boolean open;
    @XmlElement
    private Boolean unscheduled;
    private EventDto originalEvent;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GanttTaskDto that = (GanttTaskDto) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }
}
