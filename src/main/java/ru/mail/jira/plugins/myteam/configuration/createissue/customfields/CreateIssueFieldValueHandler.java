/* (C)2021 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import java.util.List;
import java.util.Locale;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.FillingIssueFieldState;

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
   * @param state state containing field, its value and filling state parameters such as paging
   * @param locale user locale
   * @return Message to shown in Myteam
   */
  String getInsertFieldMessage(
      Project project,
      IssueType issueType,
      FillingIssueFieldState state,
      ApplicationUser user,
      Locale locale);

  /**
   * Custom buttons setup attached to issue creation message
   *
   * @param fillingFieldState state containing field, its value and filling state parameters such as
   *     paging
   * @param locale user locale
   * @return Buttons to shown in Myteam
   */
  default List<List<InlineKeyboardMarkupButton>> getButtons(
      Project project,
      IssueType issueType,
      FillingIssueFieldState fillingFieldState,
      ApplicationUser user,
      Locale locale) {
    return null;
  }

  /**
   * Map field value from String in IssueCreationDto to valid String array field value
   *
   * @param value value of custom field
   * @return valid String for field in IssueInputParameters
   */
  default String updateValue(String value, String newValue) {
    return newValue;
  }

  /**
   * Map field value from String in IssueCreationDto to valid String array field value
   *
   * @param field current custom field
   * @param locale user locale
   * @return valid String array for field in IssueInputParameters
   */
  default String[] getValueAsArray(
      String value, Field field, Project project, IssueType issueType, Locale locale) {
    return new String[] {value};
  }

  default boolean isSearchable() {
    return false;
  }
}
