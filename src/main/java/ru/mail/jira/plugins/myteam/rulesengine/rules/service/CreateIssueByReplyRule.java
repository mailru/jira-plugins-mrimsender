/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.service;

import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Rule(
    name = "Create issue by reply",
    description = "Create issue by reply if feature has been setup")
public class CreateIssueByReplyRule extends BaseRule {

  static final RuleType NAME = CommandRuleType.CreateIssueByReply;

  private final IssueCreationSettingsService issueCreationSettingsService;

  public CreateIssueByReplyRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueCreationSettingsService issueCreationSettingsService) {
    super(userChatService, rulesEngine);
    this.issueCreationSettingsService = issueCreationSettingsService;
  }

  @Condition
  public boolean isValid(
      @Fact("command") String command,
      @Fact("event") ChatMessageEvent event,
      @Fact("args") String tag) {
    return NAME.equalsName(command)
        && issueCreationSettingsService.hasChatSettings(event.getChatId(), tag);
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event, @Fact("args") String tag)
      throws MyteamServerErrorException, IOException {
    userChatService.sendMessageText(
        event.getChatId(),
        "TASK CREATED: " + issueCreationSettingsService.hasChatSettings(event.getChatId(), tag));
  }
}
