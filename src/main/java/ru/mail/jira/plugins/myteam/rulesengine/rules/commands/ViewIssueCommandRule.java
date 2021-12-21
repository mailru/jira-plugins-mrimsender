/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.commands;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import kong.unirest.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.MessageResponse;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.ViewingIssueState;

@Slf4j
@Rule(name = "/issue", description = "View issue by key")
public class ViewIssueCommandRule extends BaseRule {

  static final RuleType NAME = CommandRuleType.Issue;
  private final IssueService issueService;

  public ViewIssueCommandRule(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
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
        Issue issue =
            issueService.getIssueByUser(
                issueKey, userChatService.getJiraUserFromUserChatId(event.getUserId()));

        HttpResponse<MessageResponse> response =
            userChatService.sendMessageText(
                chatId,
                messageFormatter.createIssueSummary(issue, user),
                isGroup
                    ? null
                    : messageFormatter.getIssueButtons(
                        issue.getKey(), user, issueService.isUserWatching(issue, user)));
        updateState(chatId, issueKey);
        if (!isGroup
            && (response.getStatus() != 200
                || (response.getBody() != null && !response.getBody().isOk()))) {
          log.warn(
              "sendIssueViewToUser({}, {}, {}). Text={} Response={}",
              issueKey,
              user,
              chatId,
              messageFormatter.createIssueSummary(issue, user),
              response.getBody().toString());
        }
      } catch (IssuePermissionException e) {
        rulesEngine.fireError(ErrorRuleType.IssueNoPermission, event, e.getLocalizedMessage());

        if (!isGroup) log.error("sendIssueViewToUser({}, {}, {})", issueKey, user, chatId, e);
        userChatService.deleteState(chatId);

      } catch (IssueNotFoundException e) {
        rulesEngine.fireError(ErrorRuleType.IssueNotFound, event, e.getLocalizedMessage());
        if (!isGroup) log.error("sendIssueViewToUser({}, {}, {})", issueKey, user, chatId, e);
        userChatService.deleteState(chatId);
      }
    }
  }

  private void updateState(String chatId, String issueKey) {
    BotState state = userChatService.getState(chatId);

    if (state instanceof ViewingIssueState) {
      ((ViewingIssueState) state).setIssueKey(issueKey);
      state.setWaiting(false);
    } else {
      userChatService.setState(chatId, new ViewingIssueState(issueKey));
    }
  }
}
