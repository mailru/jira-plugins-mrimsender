package ru.mail.jira.plugins.calendar.rest.dto.gantt;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Getter @AllArgsConstructor
@XmlRootElement
public class GanttActionResponse<T> {
    @XmlElement
    private final Action action;
    @XmlElement
    private final T tid;

    public enum Action {
        inserted,
        updated,
        deleted
    }
}
