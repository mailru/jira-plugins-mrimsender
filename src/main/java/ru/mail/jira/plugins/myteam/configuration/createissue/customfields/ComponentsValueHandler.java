/* (C)2022 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.issue.fields.ComponentsSystemField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.*;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

public class ComponentsValueHandler implements CreateIssueFieldValueHandler {
  private final ProjectComponentManager projectComponentManager;
  private final I18nResolver i18nResolver;
  private static final String delimiter = ", ";

  public ComponentsValueHandler(
      ProjectComponentManager projectComponentManager, I18nResolver i18nResolver) {
    this.projectComponentManager = projectComponentManager;
    this.i18nResolver = i18nResolver;
  }

  @Override
  public String getClassName() {
    return ComponentsSystemField.class.getName();
  }

  @Override
  public String getInsertFieldMessage(Field field, Locale locale) {
    i18nResolver.getText("aa");
    return String.format("Ввведите значение для поля %s", field.getName());
  }

  @Override
  public List<List<InlineKeyboardMarkupButton>> getButtons(
      Field field, Project project, IssueType issueType, String value, Locale locale) {
    return null;
  }

  @Override
  public String updateValue(String oldValue, String newValue) {

    if (oldValue.equals(newValue)) return newValue;

    List<String> values = new ArrayList<>(Arrays.asList(oldValue.split(delimiter)));

    if (values.contains(newValue)) {
      values.remove(newValue);
    } else {
      if (oldValue.length() == 0) {
        return newValue;
      } else {
        values.add(newValue);
      }
    }
    return String.join(delimiter, values);
  }

  @Override
  public String[] getValueAsArray(
      String value, Field field, Project project, IssueType issueType, Locale locale) {
    List<String> values = new ArrayList<>(Arrays.asList(value.split(delimiter)));

    Collection<ProjectComponent> components =
        projectComponentManager.findAllActiveForProject(project.getId());

    return values.stream()
        .map(
            v -> {
              ProjectComponent opt = getOption(components, v.trim());
              return opt == null ? null : String.valueOf(opt.getName());
            })
        .filter(Objects::nonNull)
        .toArray(String[]::new);
  }

  public ProjectComponent getOption(Collection<ProjectComponent> options, String value) {

    for (ProjectComponent opt : options) {
      if (opt.getName().equals(value) || String.valueOf(opt.getId()).equals(value)) {
        return opt;
      }
    }
    return null;
  }
}
