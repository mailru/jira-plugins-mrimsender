/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules;

import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

public class BaseRule {
  protected final MessageFormatter messageFormatter;
  protected final RulesEngine rulesEngine;
  protected final UserChatService userChatService;

  public BaseRule(UserChatService userChatService, RulesEngine rulesEngine) {
    this.userChatService = userChatService;
    this.rulesEngine = rulesEngine;
    messageFormatter = userChatService.getMessageFormatter();
  }

  @SuppressWarnings("EmptyCatch")
  public void answerButtonCallback(MyteamEvent event) {
    if (event instanceof ButtonClickEvent) {
      try {
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
      } catch (MyteamServerErrorException ignored) {

      }
    }
  }
}
