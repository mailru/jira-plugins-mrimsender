/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules;

import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.i18n.I18nProperty;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

import java.util.function.Supplier;

@Slf4j
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

  public void sendMessageWithRawText(final MyteamEvent event, final I18nProperty i18nProperty) {
   try {
     userChatService.sendMessageText(event.getChatId(), userChatService.getRawText(i18nProperty.getMessageKey()));
   } catch (Exception e) {
     log.error(String.format("error happened during send message to user with id [%s]", event.getUserId()), e);
   }
  }

  public void sendMessageWithFormattedText(final MyteamEvent event, final I18nProperty i18nProperty, final String data) {
    try {
      userChatService.sendMessageText(event.getChatId(), userChatService.getText(i18nProperty.getMessageKey(), data));
    } catch (Exception e) {
      log.error(String.format("error happened during send message to user with id [%s]", event.getUserId()), e);
    }
  }
}
