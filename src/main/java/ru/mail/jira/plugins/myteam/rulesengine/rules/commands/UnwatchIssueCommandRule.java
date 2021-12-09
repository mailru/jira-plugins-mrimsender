/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.commands;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IssueWatchingException;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Slf4j
@Rule(name = "/unwatch", description = "Stop watching the issue")
public class UnwatchIssueCommandRule extends BaseRule {
  static final RuleType NAME = CommandRuleType.UnwatchIssue;
  private final IssueService issueService;

  public UnwatchIssueCommandRule(UserChatService userChatService, IssueService issueService) {
    super(userChatService);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String issueKey)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    String chatId = event.getChatId();
    if (user != null) {
      Locale locale = userChatService.getUserLocale(user);
      try {
        issueService.unwatchIssue(issueKey, user);
        userChatService.sendMessageText(
            chatId,
            userChatService.getText(
                locale,
                "ru.mail.jira.plugins.myteam.messageQueueProcessor.issueWatching.successfullyUnwatch",
                messageFormatter.createIssueLink(issueKey)));
      } catch (IssueWatchingException e) {
        log.error(e.getLocalizedMessage());
        userChatService.sendMessageText(
            chatId,
            userChatService.getText(
                locale,
                "ru.mail.jira.plugins.myteam.messageQueueProcessor.issueWatching.alreadyUnwatching",
                messageFormatter.createIssueLink(issueKey)));
      } catch (IssuePermissionException e) {
        log.error(e.getLocalizedMessage());
        userChatService.sendMessageText(
            chatId,
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
      } catch (IssueNotFoundException e) {
        log.error(e.getLocalizedMessage());
        userChatService.sendMessageText(
            chatId,
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.issueNotFound"));
      }
    }
    if (event instanceof ButtonClickEvent) {
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
    }
  }
}
