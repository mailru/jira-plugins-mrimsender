/* (C)2021 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.*;
import java.util.stream.Collectors;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.rulesengine.core.Utils;

public class DefaultFieldValueHandler implements CreateIssueFieldValueHandler {

  private final I18nResolver i18nResolver;
  private final VersionManager versionManager;
  private final ProjectComponentManager projectComponentManager;

  public DefaultFieldValueHandler(I18nResolver i18nResolver) {
    this.i18nResolver = i18nResolver;
    versionManager = ComponentAccessor.getVersionManager();
    projectComponentManager = ComponentAccessor.getProjectComponentManager();
  }

  @Override
  public String getClassName() {
    return null;
  }

  @Override
  public String getInsertFieldMessage(Field field, Locale locale) {
    if (Utils.isArrayLikeField(field)) {
      return i18nResolver.getText(
          locale,
          "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.arrayMessage",
          i18nResolver.getRawText(locale, field.getNameKey()).toLowerCase(locale));
    }
    return i18nResolver.getText(
        locale,
        "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.message",
        i18nResolver.getRawText(locale, field.getNameKey()).toLowerCase(locale));
  }

  @Override
  public List<List<InlineKeyboardMarkupButton>> getButtons(
      Field field, String value, Locale locale) {
    return null;
  }

  @Override
  public String updateValue(String value, String newValue) {
    return newValue;
  }

  @Override
  public String[] getValueAsArray(String value, Field field, Project project, Locale locale) {
    return mapStringToArrayFieldValue(project.getId(), field, value);
  }

  private String[] mapStringToArrayFieldValue(Long projectId, Field field, String fieldValue) {
    List<String> fieldValues =
        Arrays.stream(fieldValue.split(",")).map(String::trim).collect(Collectors.toList());

    switch (field.getId()) {
      case IssueFieldConstants.FIX_FOR_VERSIONS:
      case IssueFieldConstants.AFFECTED_VERSIONS:
        return fieldValues.stream()
            .map(strValue -> versionManager.getVersion(projectId, strValue))
            .filter(Objects::nonNull)
            .map(version -> version.getId().toString())
            .toArray(String[]::new);
      case IssueFieldConstants.COMPONENTS:
        return fieldValues.stream()
            .map(
                strValue ->
                    Optional.ofNullable(
                            projectComponentManager.findByComponentName(projectId, strValue))
                        .map(projectComponent -> projectComponent.getId().toString())
                        .orElse(null))
            .toArray(String[]::new);

      case IssueFieldConstants.ISSUE_LINKS:
        //                IssueLinksSystemField issueLinksSystemField = (IssueLinksSystemField)
        // field;
        // hmmm....  well to parse input strings to IssueLinksSystemField.IssueFieldValue we should
        // strict user input format
        break;
      case IssueFieldConstants.LABELS:
        // TODO find existing labels via some labelManager or labelSearchers,
        //  right now label search methods, without issue parameter, don't exist
        /*return fieldValues.stream()
        .map(strValue -> labelManager.getSuggestedLabels())
        .filter(Objects::nonNull)
        .map(label -> label.getId().toString())
        .toArray(String[]::new);*/
        return fieldValues.toArray(new String[0]);
    }
    return fieldValues.toArray(new String[0]);
  }
}
