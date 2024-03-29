/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands;

import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "/help", description = "Shows help")
public class HelpCommandRule extends BaseRule {

  static final RuleType NAME = CommandRuleType.Help;

  public HelpCommandRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event)
      throws MyteamServerErrorException, IOException {
    if (event.getChatType() == ChatType.GROUP)
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              "ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.helpMessage.text"));
    else
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              "ru.mail.jira.plugins.myteam.myteamEventsListener.helpMessage.text"));
  }
}
