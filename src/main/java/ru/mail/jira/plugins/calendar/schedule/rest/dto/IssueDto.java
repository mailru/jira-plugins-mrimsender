package ru.mail.jira.plugins.calendar.schedule.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class IssueDto {
    @XmlElement
    private long id;
    @XmlElement
    private String key;
    @XmlElement
    private String summary;

    public IssueDto(long id, String key, String summary) {
        this.id = id;
        this.key = key;
        this.summary = summary;
    }
}
