/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.configuration.createissue.customfields;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.*;
import java.util.stream.Collectors;
import ru.mail.jira.plugins.myteam.bot.configuration.createissue.FieldInputMessageInfo;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.FillingIssueFieldState;

public class DefaultFieldValueHandler implements CreateIssueFieldValueHandler {

  private final I18nResolver i18nResolver;
  private final VersionManager versionManager;
  private final ProjectComponentManager projectComponentManager;
  private final ConstantsManager constantsManager;
  private final IssueSecurityLevelManager issueSecurityLevelManager;

  public DefaultFieldValueHandler(I18nResolver i18nResolver) {
    this.i18nResolver = i18nResolver;
    versionManager = ComponentAccessor.getVersionManager();
    projectComponentManager = ComponentAccessor.getProjectComponentManager();
    constantsManager = ComponentAccessor.getConstantsManager();
    issueSecurityLevelManager = ComponentAccessor.getIssueSecurityLevelManager();
  }

  @Override
  public String getClassName() {
    return null;
  }

  @Override
  public FieldInputMessageInfo getMessageInfo(
      Project project, IssueType issueType, ApplicationUser user, FillingIssueFieldState state) {
    return FieldInputMessageInfo.builder().message(getInsertFieldMessage(state)).build();
  }

  private String getInsertFieldMessage(FillingIssueFieldState state) {
    if (isArrayLikeField(state.getField())) {
      return i18nResolver.getText(
          "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.arrayMessage",
          i18nResolver.getRawText(state.getField().getNameKey()).toLowerCase());
    }
    return i18nResolver.getText(
        "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.message",
        i18nResolver.getRawText(state.getField().getNameKey()).toLowerCase());
  }

  @Override
  public String[] getValueAsArray(String value, Field field, Project project, IssueType issueType) {
    return mapUserInputStringToFieldValue(project.getId(), field, value);
  }

  // TODO custom handlers for all system fiels
  private String[] mapUserInputStringToFieldValue(Long projectId, Field field, String fieldValue) {
    if (isArrayLikeField(field)) {
      return mapStringToArrayFieldValue(projectId, field, fieldValue);
    }
    return mapStringToSingleFieldValue(field, fieldValue);
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
                (strValue) -> {
                  ProjectComponent component =
                      projectComponentManager.getProjectComponent(Long.valueOf(strValue));
                  if (component == null) {
                    component = projectComponentManager.findByComponentName(projectId, strValue);
                  }
                  return Optional.ofNullable(component);
                })
            .filter(Optional::isPresent)
            .map(projectComponent -> projectComponent.get().getId().toString())
            .toArray(String[]::new);

      case IssueFieldConstants.ISSUE_LINKS:
        //                IssueLinksSystemField issueLinksSystemField = (IssueLinksSystemField)
        // state;
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

  private String[] mapStringToSingleFieldValue(Field field, String fieldValue) {
    // no preprocessing for description and summary fields needed
    if (field.getId().equals(IssueFieldConstants.DESCRIPTION)
        || field.getId().equals(IssueFieldConstants.SUMMARY)) {
      return new String[] {fieldValue};
    }

    List<String> fieldValues =
        Arrays.stream(fieldValue.split(",")).map(String::trim).collect(Collectors.toList());

    // this state list was made based on information of which fields implements
    // AbstractOrderableField.getRelevantParams method
    switch (field.getId()) {
      case IssueFieldConstants.ASSIGNEE:
        // no additional mapping needed
        break;
      case IssueFieldConstants.ATTACHMENT:
        // not supported right now
        return new String[0];
      case IssueFieldConstants.COMMENT:
        // TODO internally uses some additional map keys for mapping comment level
        //  and comment editing/creating/removing
        break;
      case IssueFieldConstants.DUE_DATE:
        // no additional mapping needed ???
        // TODO maybe inserted user input should be mapped additionally to jira internal date format
        break;
      case IssueFieldConstants.REPORTER:
        // no additional mapping needed
        break;
      case IssueFieldConstants.RESOLUTION:
        if (!fieldValues.isEmpty()) {
          String resolutionStrValue = fieldValues.get(0);
          String selectedResolutionId =
              constantsManager.getResolutions().stream()
                  .filter(
                      resolution ->
                          resolution.getName().equals(resolutionStrValue)
                              || resolution.getNameTranslation().equals(resolutionStrValue))
                  .findFirst()
                  .map(IssueConstant::getId)
                  .orElse("");
          return new String[] {selectedResolutionId};
        }
        break;
      case IssueFieldConstants.SECURITY:
        if (!fieldValues.isEmpty()) {
          String issueSecurityLevelName = fieldValues.get(0);
          String selectedResolutionId =
              issueSecurityLevelManager.getIssueSecurityLevelsByName(issueSecurityLevelName)
                  .stream()
                  .findFirst()
                  .map(securityLevel -> Long.toString(securityLevel.getId()))
                  .orElse("");
          return new String[] {selectedResolutionId};
        }
        break;
      case IssueFieldConstants.TIMETRACKING:
        // TODO internally uses some additional map keys for mapping timetracking
        break;
      case IssueFieldConstants.WORKLOG:
        // TODO should we map this ???
        break;
    }
    return fieldValues.toArray(new String[0]);
  }

  private static boolean isArrayLikeField(Field field) {
    switch (field.getId()) {
      case IssueFieldConstants.FIX_FOR_VERSIONS:
      case IssueFieldConstants.COMPONENTS:
      case IssueFieldConstants.AFFECTED_VERSIONS:
      case IssueFieldConstants.ISSUE_LINKS:
      case IssueFieldConstants.LABELS:
      case IssueFieldConstants.VOTES:
        // never shown on issue creation screen
      case IssueFieldConstants.WATCHES:
        return true;
    }
    return false;
  }
}
