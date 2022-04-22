/* (C)2021 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.issue.IssueConstant;
import com.atlassian.jira.issue.context.IssueContextImpl;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.PrioritySystemField;
import com.atlassian.jira.issue.fields.config.manager.PrioritySchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.core.Utils;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.FillingIssueFieldState;

public class PriorityValueHandler implements CreateIssueFieldValueHandler {
  private final I18nResolver i18nResolver;
  private final PrioritySchemeManager prioritySchemeManager;

  public PriorityValueHandler(
      I18nResolver i18nResolver, PrioritySchemeManager prioritySchemeManager) {
    this.i18nResolver = i18nResolver;
    this.prioritySchemeManager = prioritySchemeManager;
  }

  @Override
  public String getClassName() {
    return PrioritySystemField.class.getName();
  }

  @Override
  public String getInsertFieldMessage(FillingIssueFieldState state, Locale locale) {
    if (Utils.isArrayLikeField(state.getField())) {
      return i18nResolver.getText(
          locale,
          "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.arrayMessage",
          i18nResolver.getRawText(locale, state.getField().getNameKey()).toLowerCase(locale));
    }

    return i18nResolver.getText(
        locale,
        "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.message",
        i18nResolver.getRawText(locale, state.getField().getNameKey()).toLowerCase(locale));
  }

  @Override
  public List<List<InlineKeyboardMarkupButton>> getButtons(
      Project project,
      IssueType issueType,
      FillingIssueFieldState fillingFieldState,
      ApplicationUser user,
      Locale locale) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    Collection<Priority> priorities = getPriorities(project, issueType);
    priorities.forEach(
        priority -> {
          InlineKeyboardMarkupButton issueTypeButton =
              InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                  priority.getNameTranslation(locale.getLanguage()),
                  String.join(
                      "-",
                      StateActionRuleType.SelectIssueCreationValue.getName(),
                      priority.getName()));
          MessageFormatter.addRowWithButton(buttons, issueTypeButton);
        });
    return buttons;
  }

  @Override
  public String[] getValueAsArray(
      String value, Field field, Project project, IssueType issueType, Locale locale) {
    String selectedPriorityId =
        getPriorities(project, issueType).stream()
            .filter(
                priority ->
                    priority.getId().equals(value)
                        || priority.getName().equals(value)
                        || priority.getNameTranslation(locale.getLanguage()).equals(value))
            .findFirst()
            .map(IssueConstant::getId)
            .orElse("");
    return new String[] {selectedPriorityId};
  }

  @NotNull
  private Collection<Priority> getPriorities(Project project, IssueType issueType) {
    return prioritySchemeManager.getPrioritiesFromIds(
        prioritySchemeManager.getOptions(new IssueContextImpl(project.getId(), issueType.getId())));
  }
}
