/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.service.CommonButtonsService;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.ViewingIssueState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@SuppressWarnings("UnusedVariable")
@Rule(name = "/issue", description = "View issue by key")
public class ViewIssueCommandRule extends BaseRule {

  static final RuleType NAME = CommandRuleType.Issue;
  private final CommonButtonsService commonButtonsService;
  private final IssueService issueService;

  public ViewIssueCommandRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      CommonButtonsService commonButtonsService,
      IssueService issueService) {
    super(userChatService, rulesEngine);
    this.commonButtonsService = commonButtonsService;
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(
      @Fact("event") MyteamEvent event,
      @Fact("args") String issueKey,
      @Fact("isGroup") boolean isGroup)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (issueKey.length() > 0 && user != null) {
      String chatId = event.getChatId();
      try {
        Issue issue = issueService.getIssueByUser(issueKey, user);

        userChatService.sendMessageText(
            chatId,
            messageFormatter.createIssueSummary(issue, user),
            isGroup
                ? null
                : commonButtonsService.getIssueButtons(
                    issue.getKey(), user, issueService.isUserWatching(issue, user)));
        updateState(chatId, issueKey);
      } catch (IssuePermissionException e) {
        rulesEngine.fireError(ErrorRuleType.IssueNoPermission, event, e);
        userChatService.deleteState(chatId);

      } catch (IssueNotFoundException e) {
        rulesEngine.fireError(ErrorRuleType.IssueNotFound, event, e);
        userChatService.deleteState(chatId);
      }
    }

    if (event instanceof ButtonClickEvent) {
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
    }
  }

  private void updateState(String chatId, String issueKey) {
    BotState state = userChatService.getState(chatId);

    if (state instanceof ViewingIssueState) {
      ((ViewingIssueState) state).setIssueKey(issueKey);
      state.setWaiting(false);
    } else {
      userChatService.setState(chatId, new ViewingIssueState(userChatService, issueKey));
    }
  }
}
