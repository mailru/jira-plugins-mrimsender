/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.commands.admin;

import java.io.IOException;
import java.util.Collections;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.ChatAdminRule;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "configure_task", description = "Configures issues creation in chat")
public class IssueCreationSettingsCommand extends ChatAdminRule {
  static final RuleType NAME = CommandRuleType.IssueCreationSettings;

  private final IssueService issueService;

  public IssueCreationSettingsCommand(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(
      @Fact("isGroup") boolean isGroup,
      @Fact("event") MyteamEvent event,
      @Fact("command") String command)
      throws AdminRulesRequiredException {
    if (!isGroup) {
      return false;
    }
    checkAdminRules(event);
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event)
      throws MyteamServerErrorException, IOException {
    userChatService.deleteMessages(
        event.getChatId(), Collections.singletonList(event.getMessageId()));

    String chatId = event.getChatId();

    String link =
        String.format("%s/myteam/chats/settings?chatId=%s", issueService.getJiraBaseUrl(), chatId);

    userChatService.sendMessageText(
        event.getUserId(), "Visit this page to edit chat issue creation settings: \n\n" + link);
  }
}
