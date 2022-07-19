/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states.base;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.UserChatService;

public interface CancelableState {

  default UserChatService getUserChatService() {
    return null;
  }

  default void onError(Exception e) {}

  default void cancel(MyteamEvent event) {
    UserChatService userChatService = getUserChatService();
    userChatService.deleteState(event.getChatId());

    if (event instanceof ButtonClickEvent) {
      try {
        ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getChatId());
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
        userChatService.editMessageText(
            event.getChatId(),
            ((ButtonClickEvent) event).getMsgId(),
            userChatService.getRawText(
                "ru.mail.jira.plugins.myteam.myteamEventsListener.actionCanceled"),
            null);
      } catch (MyteamServerErrorException | IOException e) {
        onError(e);
      }
    }
  };
}
