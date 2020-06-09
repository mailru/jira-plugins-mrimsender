package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.issue.fields.Field;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class IssueCreationDto {
    private Long projectId;
    private String issueTypeId;
    private Map<Field, String> requiredIssueCreationFieldValues;
}
