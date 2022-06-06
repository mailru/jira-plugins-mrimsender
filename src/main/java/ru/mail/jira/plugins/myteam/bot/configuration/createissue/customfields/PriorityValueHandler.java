/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.configuration.createissue.customfields;

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
import ru.mail.jira.plugins.myteam.bot.configuration.createissue.FieldInputMessageInfo;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issuecreation.FillingIssueFieldState;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

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
  public FieldInputMessageInfo getMessageInfo(
      Project project,
      IssueType issueType,
      ApplicationUser user,
      Locale locale,
      FillingIssueFieldState state) {
    return FieldInputMessageInfo.builder()
        .message(getInsertFieldMessage(state, locale))
        .buttons(getButtons(project, issueType, locale))
        .build();
  }

  private String getInsertFieldMessage(FillingIssueFieldState state, Locale locale) {
    return i18nResolver.getText(
        locale,
        "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.message",
        i18nResolver.getRawText(locale, state.getField().getNameKey()).toLowerCase(locale));
  }

  private List<List<InlineKeyboardMarkupButton>> getButtons(
      Project project, IssueType issueType, Locale locale) {
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

  private Collection<Priority> getPriorities(Project project, IssueType issueType) {
    return prioritySchemeManager.getPrioritiesFromIds(
        prioritySchemeManager.getOptions(new IssueContextImpl(project.getId(), issueType.getId())));
  }
}