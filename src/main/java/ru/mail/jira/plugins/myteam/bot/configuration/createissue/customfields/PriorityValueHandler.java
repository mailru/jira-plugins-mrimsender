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
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.configuration.createissue.FieldInputMessageInfo;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.FillingIssueFieldState;
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
  public @Nullable String getClassName() {
    return PrioritySystemField.class.getName();
  }

  @Override
  public FieldInputMessageInfo getMessageInfo(
      Project project,
      IssueType issueType,
      @Nullable ApplicationUser user,
      FillingIssueFieldState state) {
    return FieldInputMessageInfo.builder()
        .message(getInsertFieldMessage(state))
        .buttons(getButtons(project, issueType))
        .build();
  }

  private String getInsertFieldMessage(FillingIssueFieldState state) {
    return i18nResolver.getText(
        "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.message",
        i18nResolver.getRawText(state.getField().getNameKey()).toLowerCase());
  }

  private List<List<InlineKeyboardMarkupButton>> getButtons(Project project, IssueType issueType) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    Collection<Priority> priorities = getPriorities(project, issueType);
    priorities.forEach(
        priority -> {
          InlineKeyboardMarkupButton issueTypeButton =
              InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                  priority.getNameTranslation(),
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
      @Nullable String value, Field field, Project project, IssueType issueType) {
    String selectedPriorityId =
        getPriorities(project, issueType).stream()
            .filter(
                priority ->
                    priority.getId().equals(value)
                        || priority.getName().equals(value)
                        || priority.getNameTranslation().equals(value))
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
