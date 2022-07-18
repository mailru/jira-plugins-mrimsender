/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "/menu", description = "Shows menu")
public class MenuCommandRule extends BaseRule {
  static final RuleType NAME = CommandRuleType.Menu;

  public MenuCommandRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    Locale locale = userChatService.getCtxUserLocale();
    userChatService.sendMessageText(
        event.getChatId(),
        userChatService.getRawText(
            locale, "ru.mail.jira.plugins.myteam.messageQueueProcessor.mainMenu.text"),
        messageFormatter.getMenuButtons(user));

    if (event instanceof ButtonClickEvent) {
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
    }
  }
}
