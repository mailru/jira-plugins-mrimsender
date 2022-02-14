/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
public class ViewingIssueState extends BotState implements CancelableState {
  @Getter @Setter private String issueKey;
  private final UserChatService userChatService;

  public ViewingIssueState(UserChatService userChatService, String issueKey) {
    this.issueKey = issueKey;
    this.userChatService = userChatService;
  }

  @Override
  public void cancel(MyteamEvent event) {
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
        log.error(e.getLocalizedMessage(), e);
      }
    }
  }
}
