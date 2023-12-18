/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.buttons;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestDto;
import ru.mail.jira.plugins.myteam.accessrequest.service.AccessRequestService;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "reply", description = "reply user access")
public class ReplyRule extends BaseRule {
  private enum ReplyArgs {
    COMMAND_ARG,
    HISTORY_ARG,
    USERKEY_ARG,
  }

  public enum ReplyCommands {
    COMMAND_ALLOW,
    COMMAND_FORBID
  }

  public static final String COMMAND_ALLOW = "allow";
  public static final String COMMAND_FORBID = "forbid";
  static final ButtonRuleType NAME = ButtonRuleType.AccessReply;
  private final AccessRequestService accessRequestService;

  public ReplyRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      AccessRequestService accessRequestService) {
    super(userChatService, rulesEngine);
    this.accessRequestService = accessRequestService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event, @Fact("args") String args)
      throws MyteamServerErrorException, IOException {

    List<String> parsedArgs = RuleType.parseArgs(args);
    ReplyCommands replyCommand =
        ReplyCommands.valueOf(parsedArgs.get(ReplyArgs.COMMAND_ARG.ordinal()));
    int historyId = Integer.parseInt(parsedArgs.get(ReplyArgs.HISTORY_ARG.ordinal()));
    String userKey = parsedArgs.get(ReplyArgs.USERKEY_ARG.ordinal());

    AccessRequestDto accessRequestDto = accessRequestService.getAccessRequestHistoryDto(historyId);

    if (accessRequestDto == null) return;

    if (accessRequestDto.getReplyStatus() == null) {
      updateAccessRequest(accessRequestDto, replyCommand, userKey, historyId);
    } else {
      ReplyCommands prevReplyCommand =
          accessRequestDto.getReplyStatus()
              ? ReplyCommands.COMMAND_ALLOW
              : ReplyCommands.COMMAND_FORBID;
      replyAdminUsers(prevReplyCommand, event.getChatId());
    }
    userChatService.answerCallbackQuery(event.getQueryId());
  }

  private void replyAdminUsers(ReplyCommands replyCommands, String chatId) {
    String message = "";
    if (replyCommands.equals(ReplyCommands.COMMAND_ALLOW)) {
      message = "Пользователь уже добавлен";
    } else {
      message = "Пользователь уже отклонен";
    }
    try {
      userChatService.sendMessageText(chatId, message);
    } catch (Exception e) {
      SentryClient.capture(e, Map.of("user", chatId));
    }
  }

  private void updateAccessRequest(
      AccessRequestDto accessRequestDto,
      ReplyCommands replyCommand,
      String userKey,
      int historyId) {
    UserDto userDto =
        accessRequestDto.getUsers().stream()
            .filter(user -> user.getUserKey().equals(userKey))
            .findAny()
            .orElse(null);

    if (userDto != null) {
      accessRequestDto.setReplyAdmin(userDto);
      accessRequestDto.setReplyDate(Utils.convertToDate(LocalDateTime.now(ZoneId.systemDefault())));
      accessRequestDto.setReplyStatus(replyCommand.equals(ReplyCommands.COMMAND_ALLOW));
    }
    accessRequestService.updateAccessHistory(historyId, accessRequestDto);
  }
}
