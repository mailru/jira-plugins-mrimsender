/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import ru.mail.jira.plugins.myteam.commons.IssueFieldsFilter;
import ru.mail.jira.plugins.myteam.configuration.createissue.customfields.CreateIssueFieldValueHandler;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IncorrectIssueTypeException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IssueCreationValidationException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.ProjectBannedException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.UnsupportedCustomFieldsException;

public interface IssueCreationService {
  /**
   * Get a map of required during creation issue fields by selected project and issue type Algo: 1)
   * Getting all fields which shown on CREATE_ISSUE_OPERATION fields screen 2) All fields must be
   * required in FieldLayout => filtration 3) All custom fields must have correct context scope to
   * be shown on issue creation form
   *
   * @param project - selected project
   * @param issueType - selected issue type
   * @param includedFieldIds - fields which must be included in result map no matter required them
   *     or not
   * @param excludedFieldIds - fields which must be excluded from result map no matter required them
   *     or not
   * @param issueFieldsFilter - which fields needs to be filtered
   * @return LinkedHashMap of field and empty string value
   */
  LinkedHashMap<Field, String> getIssueCreationFieldsValues(
      Project project,
      IssueType issueType,
      Set<String> includedFieldIds,
      Set<String> excludedFieldIds,
      IssueFieldsFilter issueFieldsFilter);

  CreateIssueFieldValueHandler getFieldValueHandler(Field field);

  List<Field> getIssueFields(Project project, ApplicationUser user, String issueTypeId)
      throws UnsupportedCustomFieldsException, IncorrectIssueTypeException;

  LinkedHashMap<Field, String> getRequiredIssueFields(
      Project project, ApplicationUser user, String issueTypeId);

  Issue createIssue(
      Project project,
      IssueType issueType,
      @NotNull Map<Field, String> fields,
      ApplicationUser user)
      throws IssueCreationValidationException;

  Issue createIssue(
      String projectKey,
      String issueTypeId,
      @NotNull Map<Field, String> fields,
      ApplicationUser user)
      throws IssueCreationValidationException, PermissionException, ProjectBannedException;

  boolean isFieldSupported(String fieldId);

  Field getField(String fieldId);
}
