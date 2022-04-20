/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states.base;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
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
        Locale locale = userChatService.getUserLocale(user);
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
        userChatService.editMessageText(
            event.getChatId(),
            ((ButtonClickEvent) event).getMsgId(),
            userChatService.getRawText(
                locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.actionCanceled"),
            null);
      } catch (MyteamServerErrorException | UserNotFoundException | IOException e) {
        onError(e);
      }
    }
  };
}
