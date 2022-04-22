/* (C)2022 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import java.util.List;
import java.util.Locale;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

public class EpicLinkValueHandler implements CreateIssueFieldValueHandler {
  @Override
  public String getClassName() {
    return "com.atlassian.greenhopper.customfield.epiclink.EpicLinkCFType";
  }

  @Override
  public String getInsertFieldMessage(Field field, Locale locale) {
    return null;
  }

  @Override
  public List<List<InlineKeyboardMarkupButton>> getButtons(
      Field field, Project project, IssueType issueType, String value, Locale locale) {
    return null;
  }

  @Override
  public String updateValue(String value, String newValue) {
    return null;
  }

  @Override
  public String[] getValueAsArray(
      String value, Field field, Project project, IssueType issueType, Locale locale) {
    return new String[0];
  }
}
