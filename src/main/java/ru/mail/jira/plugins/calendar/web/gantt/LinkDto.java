package ru.mail.jira.plugins.calendar.web.gantt;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LinkDto {
    private String typeName;
    private String issueKey;
    private String issueSummary;
    private String issueTypeIconUrl;
    private boolean outbound;
}
