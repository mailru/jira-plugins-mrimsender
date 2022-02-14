/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.commands.admin;

import java.io.IOException;
import java.util.Collections;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.GroupAdminRule;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "configure_task", description = "Configures issues creation in chat")
public class IssueCreationSettingsCommand extends GroupAdminRule {
  static final RuleType NAME = CommandRuleType.IssueCreationSettings;

  private final IssueCreationSettingsService issueCreationSettingsService;
  private final IssueService issueService;

  public IssueCreationSettingsCommand(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueCreationSettingsService issueCreationSettingsService,
      IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueCreationSettingsService = issueCreationSettingsService;
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

    IssueCreationSettingsDto settings =
        issueCreationSettingsService
            .getSettingsByChatId(chatId)
            .orElseGet(() -> issueCreationSettingsService.addDefaultSettings(chatId));

    String link =
        String.format(
            "%s/myteam/chats/settings?chatId=%s",
            issueService.getJiraBaseUrl(), settings.getChatId());

    userChatService.sendMessageText(
        event.getUserId(), "Visit this page to edit chat issue creation settings: \n\n" + link);
  }
}
