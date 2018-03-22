package ru.mail.jira.plugins.calendar.service.applications;

import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;

public class JiraSoftwareHelperStub implements JiraSoftwareHelper {
    public JiraSoftwareHelperStub() {}

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public CustomField getEpicLinkField() {
        throw new UnsupportedOperationException();
    }

    @Override
    public CustomField getRankField() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IssueType getEpicIssueType() {
        throw new UnsupportedOperationException();
    }
}
