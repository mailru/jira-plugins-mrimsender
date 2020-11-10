/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import com.atlassian.jira.issue.fields.Field;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueCreationDto {
  private Long projectId;
  private String issueTypeId;
  private Map<Field, String> requiredIssueCreationFieldValues;
}
