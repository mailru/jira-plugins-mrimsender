/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.ChatAdminRule;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;
import ru.mail.jira.plugins.myteam.myteam.dto.response.AdminsResponse;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "/pin", description = "pin message")
public class PinMessageCommandRule extends ChatAdminRule {
  private final MyteamApiClient myteamApiClient;

  public PinMessageCommandRule(
      final UserChatService userChatService,
      final RulesEngine rulesEngine,
      final MyteamApiClient myteamApiClient) {
    super(userChatService, rulesEngine);
    this.myteamApiClient = myteamApiClient;
  }

  @Condition
  public boolean isValid(
      @Fact("command") final String command,
      @Fact("event") final MyteamEvent event,
      @Fact("isGroup") final boolean isGroup) {
    return isGroup
        && CommandRuleType.PinMessage.getName().equals(command)
        && event instanceof ChatMessageEvent;
  }

  @Action
  public void execute(@Fact("event") final ChatMessageEvent event)
      throws MyteamServerErrorException, IOException {
    if (!isJiraBotAdmin(event.getChatId())) {
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText("ru.mail.jira.plugins.myteam.pin.message.botNotAdmin"));
      return;
    }

    final List<Reply> replies =
        Optional.ofNullable(event.getMessageParts())
            .map(
                parts ->
                    parts.stream()
                        .filter(Reply.class::isInstance)
                        .map(Reply.class::cast)
                        .collect(Collectors.toUnmodifiableList()))
            .orElse(new ArrayList<>());
    if (replies.size() == 1) {
      myteamApiClient.pinMessage(replies.get(0).getMessage().getMsgId(), event.getChatId());
    } else {
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText("ru.mail.jira.plugins.myteam.pin.message.wrongSentMessage"));
    }
  }

  private boolean isJiraBotAdmin(final String chatId) throws MyteamServerErrorException {
    final String botId = myteamApiClient.getBotId();
    final AdminsResponse body = myteamApiClient.getAdmins(chatId).getBody();
    return body.getAdmins().stream().anyMatch(it -> botId.equals(it.getUserId()));
  }
}
