/* (C)2021 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import java.util.List;
import java.util.Locale;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

public interface CreateIssueFieldValueHandler {
  /**
   * Function to detect custom field
   *
   * @return custom field type from customField.getCustomFieldType()
   */
  String getClassName();

  /**
   * Custom text render for Myteam message
   *
   * @param field current custom field
   * @param locale user locale
   * @return Message to shown in Myteam
   */
  String getInsertFieldMessage(Field field, Locale locale);

  /**
   * Custom buttons setup attached to issue creation message
   *
   * @param field current custom field
   * @param value String value from button or text input
   * @param locale user locale
   * @return Buttons to shown in Myteam
   */
  List<List<InlineKeyboardMarkupButton>> getButtons(
      Field field, Project project, IssueType issueType, String value, Locale locale);

  /**
   * Map field value from String in IssueCreationDto to valid String array field value
   *
   * @param value value of custom field
   * @return valid String for field in IssueInputParameters
   */
  String updateValue(String value, String newValue);

  /**
   * Map field value from String in IssueCreationDto to valid String array field value
   *
   * @param field current custom field
   * @param locale user locale
   * @return valid String array for field in IssueInputParameters
   */
  String[] getValueAsArray(
      String value, Field field, Project project, IssueType issueType, Locale locale);
}
