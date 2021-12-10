/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.listeners;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.Subscribe;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.protocol.ChatState;
import ru.mail.jira.plugins.myteam.protocol.ChatStateMapping;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.CancelClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ShowIssueClickEvent;

@Slf4j
@Component
public class ButtonClickListener {
  private final ConcurrentHashMap<String, ChatState> chatsStateMap;
  private final MyteamApiClient myteamApiClient;
  private final UserData userData;
  private final MessageFormatter messageFormatter;
  private final I18nResolver i18nResolver;
  private final LocaleManager localeManager;

  @Autowired
  public ButtonClickListener(
      ChatStateMapping chatStateMapping,
      MyteamApiClient myteamApiClient,
      UserData userData,
      MessageFormatter messageFormatter,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport LocaleManager localeManager) {
    this.chatsStateMap = chatStateMapping.getChatsStateMap();
    this.myteamApiClient = myteamApiClient;
    this.userData = userData;
    this.messageFormatter = messageFormatter;
    this.i18nResolver = i18nResolver;
    this.localeManager = localeManager;
  }

  @Subscribe
  public void onCancelButtonClick(CancelClickEvent cancelClickEvent)
      throws UnirestException, MyteamServerErrorException {
    log.debug("CancelCommand execution started...");
    String message =
        i18nResolver.getRawText(
            localeManager.getLocaleFor(userData.getUserByMrimLogin(cancelClickEvent.getUserId())),
            "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.cancelComment.message");
    myteamApiClient.answerCallbackQuery(cancelClickEvent.getQueryId(), message, false, null);
    chatsStateMap.remove(cancelClickEvent.getChatId());
    log.debug("CancelCommand execution finished...");
  }

  @Subscribe
  public void onShowIssueButtonClick(ShowIssueClickEvent showIssueClickEvent)
      throws IOException, UnirestException, MyteamServerErrorException {
    log.debug("OnSearchIssueButtonClick event handling started");
    ApplicationUser currentUser = userData.getUserByMrimLogin(showIssueClickEvent.getUserId());
    if (currentUser != null) {
      String message =
          i18nResolver.getRawText(
              localeManager.getLocaleFor(currentUser),
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.searchButton.insertIssueKey.message");
      myteamApiClient.answerCallbackQuery(showIssueClickEvent.getQueryId());
      myteamApiClient.sendMessageText(
          showIssueClickEvent.getChatId(),
          message,
          messageFormatter.getCancelButton(localeManager.getLocaleFor(currentUser)));
      chatsStateMap.put(showIssueClickEvent.getChatId(), ChatState.issueKeyWaitingState);
    }
    log.debug("OnSearchIssueButtonClick event handling finished");
  }
}
