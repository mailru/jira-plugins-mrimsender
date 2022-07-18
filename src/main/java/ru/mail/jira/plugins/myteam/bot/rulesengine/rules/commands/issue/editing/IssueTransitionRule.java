/* (C)2022 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue.editing;

import com.opensymphony.workflow.loader.ActionDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.transition.IssueTransitionEditingState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "issue transition", description = "change issue status")
public class IssueTransitionRule extends BaseRule {

  static final RuleType NAME = CommandRuleType.IssueTransition;
  private final IssueService issueService;

  public IssueTransitionRule(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event, @Fact("args") String issueKey)
      throws MyteamServerErrorException, IOException {
    userChatService.sendMessageText(
        event.getChatId(),
        userChatService.getRawText(
            userChatService.getCtxUserLocale(),
            "ru.mail.jira.plugins.myteam.messageFormatter.editIssue.transitionChange.message"),
        getTransitionButtons(issueKey));

    userChatService.setState(
        event.getChatId(),
        new IssueTransitionEditingState(issueService.getIssueByUser(issueKey), userChatService));

    userChatService.answerCallbackQuery(event.getQueryId());
  }

  private List<List<InlineKeyboardMarkupButton>> getTransitionButtons(String issueKey) {
    Collection<ActionDescriptor> transitions = issueService.getIssueTransitions(issueKey);

    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();

    transitions.forEach(
        t -> {
          List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();

          buttonsRow.add(
              InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                  t.getName(),
                  String.join(
                      "-",
                      StateActionRuleType.SelectIssueTransition.getName(),
                      String.valueOf(t.getId()))));

          buttons.add(buttonsRow);
        });
    buttons.add(messageFormatter.getCancelButtonRow(userChatService.getCtxUserLocale()));

    return buttons;
  }
}
