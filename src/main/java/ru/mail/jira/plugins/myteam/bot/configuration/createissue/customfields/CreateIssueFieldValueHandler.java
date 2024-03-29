/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.configuration.createissue.customfields;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.configuration.createissue.FieldInputMessageInfo;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.FillingIssueFieldState;
import ru.mail.jira.plugins.myteam.commons.exceptions.ValidationException;

public interface CreateIssueFieldValueHandler {
  /**
   * Function to detect custom field
   *
   * @return custom field type from customField.getCustomFieldType()
   */
  @Nullable
  String getClassName();

  /**
   * Custom text render for Myteam message and custom buttons setup attached to issue creation
   * message
   *
   * @param user application user
   * @param state state containing field, its value and filling state parameters such as paging
   * @return Message to shown in Myteam
   */
  FieldInputMessageInfo getMessageInfo(
      Project project,
      IssueType issueType,
      @Nullable ApplicationUser user,
      FillingIssueFieldState state);

  /**
   * Map field value from String in IssueCreationDto to valid String array field value
   *
   * @param value value of custom field
   * @param event myteam event
   * @return valid String for field in IssueInputParameters
   */
  default String updateValue(String value, String newValue, MyteamEvent event)
      throws ValidationException {
    return newValue;
  }

  /**
   * Map field value from String in IssueCreationDto to valid String array field value
   *
   * @param value custom field value
   * @param field current custom field
   * @return valid String array for field in IssueInputParameters
   */
  default String[] getValueAsArray(
      @Nullable String value, Field field, Project project, IssueType issueType) {
    return new String[] {value};
  }

  default boolean isSearchable() {
    return false;
  }
}
