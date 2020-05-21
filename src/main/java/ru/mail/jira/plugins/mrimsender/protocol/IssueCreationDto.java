package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.issue.fields.OrderableField;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Optional;

@Getter
@Setter
public class IssueCreationDto {
    private String projectKey;
    private String issueTypeId;
    private Map<OrderableField, Optional<String>> requiredIssueCreationFields;
}
