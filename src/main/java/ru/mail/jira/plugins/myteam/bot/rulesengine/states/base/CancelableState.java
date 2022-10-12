/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states.base;

import java.io.IOException;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.UserChatService;

public interface CancelableState {

  @Nullable
  default UserChatService getUserChatService() {
    return null;
  }

  default void onError(Exception e) {}

  default void cancel(MyteamEvent event) {
    UserChatService userChatService = getUserChatService();
    if (userChatService != null) userChatService.deleteState(event.getChatId());

    if (event instanceof ButtonClickEvent) {
      try {
        if (userChatService != null) {
          userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
          userChatService.editMessageText(
              event.getChatId(),
              ((ButtonClickEvent) event).getMsgId(),
              userChatService.getRawText(
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.actionCanceled"),
              null);
        }
      } catch (MyteamServerErrorException | IOException e) {
        onError(e);
      }
    }
  }
  ;
}
