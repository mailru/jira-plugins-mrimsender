package ru.mail.jira.plugins.calendar.service.applications;

import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.user.ApplicationUser;

import java.util.List;

public interface JiraSoftwareHelper {
    boolean isAvailable();

    CustomField getEpicLinkField();

    CustomField getRankField();

    CustomField getSprintField();

    IssueType getEpicIssueType();

    List<SprintDto> findSprints(ApplicationUser user, String query);

    SprintDto getSprint(ApplicationUser user, long id);
}
