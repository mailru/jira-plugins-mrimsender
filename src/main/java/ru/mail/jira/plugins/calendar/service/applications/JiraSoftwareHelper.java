package ru.mail.jira.plugins.calendar.service.applications;

import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;

public interface JiraSoftwareHelper {
    boolean isAvailable();

    CustomField getEpicLinkField();

    CustomField getRankField();

    IssueType getEpicIssueType();
}
