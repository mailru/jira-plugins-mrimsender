/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue;

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
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IssueWatchingException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
@Rule(name = "/unwatch", description = "Stop watching the issue")
public class UnwatchIssueCommandRule extends BaseRule {
  static final RuleType NAME = CommandRuleType.UnwatchIssue;
  private final IssueService issueService;

  public UnwatchIssueCommandRule(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String issueKey)
      throws MyteamServerErrorException, IOException {
    String chatId = event.getChatId();
    Locale locale = userChatService.getCtxUserLocale();
    try {
      issueService.unwatchIssue(issueKey);
      userChatService.sendMessageText(
          chatId,
          userChatService.getText(
              locale,
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.issueWatching.successfullyUnwatch",
              messageFormatter.createIssueLink(issueKey)));
    } catch (IssueWatchingException e) {
      log.error(e.getLocalizedMessage(), e);
      userChatService.sendMessageText(
          chatId,
          userChatService.getText(
              locale,
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.issueWatching.alreadyUnwatching",
              messageFormatter.createIssueLink(issueKey)));
    } catch (IssuePermissionException e) {
      rulesEngine.fireError(ErrorRuleType.IssueNoPermission, event, e);
    } catch (IssueNotFoundException e) {
      rulesEngine.fireError(ErrorRuleType.IssueNotFound, event, e);
    }

    if (event instanceof ButtonClickEvent) {
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
    }
  }
}
