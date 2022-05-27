/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.commands.issue;

import com.atlassian.jira.exception.IssueNotFoundException;
import java.io.IOException;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.LinkIssueWithChatException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
@Rule(name = "/link", description = "Link issue with chats")
public class LinkIssueWithChatCommandRule extends BaseRule {

  static final RuleType NAME = CommandRuleType.LinkIssueWithChat;

  public LinkIssueWithChatCommandRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String issueKey)
      throws MyteamServerErrorException, IOException {
    Locale locale = userChatService.getUserLocale(event.getUserId());
    String chatId = event.getChatId();
    try {
      userChatService.linkChat(chatId, issueKey);
      userChatService.sendMessageText(
          chatId,
          userChatService.getText(
              locale,
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.issueLinkedToChat",
              messageFormatter.createIssueLink(issueKey)));
    } catch (LinkIssueWithChatException e) {
      log.error(e.getLocalizedMessage(), e);
      userChatService.sendMessageText(
          chatId,
          userChatService.getText(
              locale,
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.issueLinkedToChat.error",
              messageFormatter.createIssueLink(issueKey)));
    } catch (IssueNotFoundException e) {
      rulesEngine.fireError(ErrorRuleType.IssueNotFound, event, e);
    }
  }
}
