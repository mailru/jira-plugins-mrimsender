package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.issue.fields.Field;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class IssueCreationDto {
    private String projectKey;
    private String issueTypeId;
    private Map<Field, String> requiredFields;

    public IssueCreationDto(String projectKey) {
        this.projectKey = projectKey;
        issueTypeId = null;
        requiredFields = null;
    }
}
