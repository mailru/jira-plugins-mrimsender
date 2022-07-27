/* (C)2022 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue.editing;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IssueTransitionException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.transition.IssueTransitionEditingState;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "select issue transition", description = "change issue by selected status")
public class IssueTransitionSelectRule extends BaseRule {

  static final RuleType NAME = StateActionRuleType.SelectIssueTransition;
  private final IssueService issueService;

  public IssueTransitionSelectRule(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command, @Fact("state") BotState state) {
    return NAME.equalsName(command) && state instanceof IssueTransitionEditingState;
  }

  @Action
  public void execute(
      @Fact("event") ButtonClickEvent event,
      @Fact("args") String transitionId,
      @Fact("state") IssueTransitionEditingState state)
      throws MyteamServerErrorException, IOException {

    @Nullable ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());

    try {
      issueService.changeIssueStatus(state.getIssue(), Integer.parseInt(transitionId), user);
      userChatService.editMessageText(
          event.getChatId(),
          event.getMsgId(),
          userChatService.getRawText(
              "ru.mail.jira.plugins.myteam.messageFormatter.editIssue.transitionChange.success"),
          null);
      userChatService.deleteState(event.getChatId());
    } catch (IssueTransitionException e) {
      userChatService.sendMessageText(
          event.getChatId(),
          String.join(
              "\n",
              userChatService.getText(
                  "ru.mail.jira.plugins.myteam.messageFormatter.editIssue.transitionChange.error",
                  e.getMessage()),
              Utils.stringifyMap(e.getErrors().getErrors()),
              Utils.stringifyCollection(e.getErrors().getErrorMessages())));
    }

    userChatService.answerCallbackQuery(event.getQueryId());
  }
}
