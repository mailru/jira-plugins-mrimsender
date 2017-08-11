package ru.mail.jira.plugins.calendar.schedule.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class ScheduleDto {
    @XmlElement
    private int id;
    @XmlElement
    private IssueDto sourceIssue;
    @XmlElement
    private String name;
    @XmlElement
    private String description;
    @XmlElement
    private UserDto creator;
    @XmlElement
    private String mode;
    @XmlElement
    private String cronExpression;
    @XmlElement
    private int runCount;
    @XmlElement
    private String lastRun;
    @XmlElement
    private IssueDto lastCreatedIssue;
    @XmlElement
    private boolean deleted;

    public ScheduleDto(int id, IssueDto sourceIssue, String name, String description, UserDto creator, String mode, String cronExpression,
                       int runCount, String lastRun, IssueDto lastCreatedIssue, boolean deleted) {
        this.id = id;
        this.sourceIssue = sourceIssue;
        this.name = name;
        this.description = description;
        this.creator = creator;
        this.mode = mode;
        this.cronExpression = cronExpression;
        this.runCount = runCount;
        this.lastRun = lastRun;
        this.lastCreatedIssue = lastCreatedIssue;
        this.deleted = deleted;
    }
}
