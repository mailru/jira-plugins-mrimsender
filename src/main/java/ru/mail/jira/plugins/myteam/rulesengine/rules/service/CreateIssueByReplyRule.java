/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;
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

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

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
      @Fact("isGroup") boolean isGroup,
      @Fact("args") String tag) {
    return isGroup
        && NAME.equalsName(command)
        && issueCreationSettingsService.hasChatSettings(event.getChatId(), tag)
        && event.getMessageParts().size() > 0;
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event, @Fact("args") String tag)
      throws MyteamServerErrorException, IOException {

    StringBuilder str = new StringBuilder();

    event.getMessageParts().stream()
        .filter(part -> part instanceof Reply)
        .forEach(
            p -> {
              User user = ((Reply) p).getMessage().getFrom();

              LocalDateTime dateTime =
                  LocalDateTime.ofInstant(
                      Instant.ofEpochSecond(((Reply) p).getMessage().getTimestamp()),
                      TimeZone.getDefault().toZoneId());

              str.append(user.getFirstName()).append(" ").append(user.getLastName()).append(" ");
              str.append("(").append(formatter.format(dateTime)).append("):\n").append("\n");
              str.append(((Reply) p).getPayload().getMessage().getText());
              str.append("\n\n");
            });

    userChatService.sendMessageText(event.getChatId(), str.toString());
  }
}
