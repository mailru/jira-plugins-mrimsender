/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue;

import com.atlassian.jira.exception.IssueNotFoundException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.LinkIssueWithChatException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.MyteamService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
@Rule(name = "/link", description = "Link issue with chats")
public class LinkIssueWithChatCommandRule extends BaseRule {

  static final RuleType NAME = CommandRuleType.LinkIssueWithChat;

  private final MyteamService myteamService;

  public LinkIssueWithChatCommandRule(
      UserChatService userChatService, RulesEngine rulesEngine, MyteamService myteamService) {
    super(userChatService, rulesEngine);
    this.myteamService = myteamService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String issueKey)
      throws MyteamServerErrorException, IOException {
    String chatId = event.getChatId();
    try {
      myteamService.linkChat(chatId, issueKey);
      userChatService.sendMessageText(
          chatId,
          userChatService.getText(
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.issueLinkedToChat",
              messageFormatter.createIssueLink(issueKey)));
    } catch (LinkIssueWithChatException e) {
      log.error(e.getLocalizedMessage(), e);
      userChatService.sendMessageText(
          chatId,
          userChatService.getText(
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.issueLinkedToChat.error",
              messageFormatter.createIssueLink(issueKey)));
    } catch (IssueNotFoundException e) {
      rulesEngine.fireError(ErrorRuleType.IssueNotFound, event, e);
    }
  }
}
