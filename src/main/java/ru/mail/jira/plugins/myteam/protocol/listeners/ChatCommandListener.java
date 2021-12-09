/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.listeners;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.Subscribe;
import java.io.IOException;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.model.MyteamChatRepository;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.MessageResponse;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.LinkIssueWithChatEvent;

@Slf4j
@Component
public class ChatCommandListener {
  private final MyteamApiClient myteamApiClient;
  private final MyteamChatRepository myteamChatRepository;
  private final UserData userData;
  private final MessageFormatter messageFormatter;
  private final I18nResolver i18nResolver;
  private final LocaleManager localeManager;
  private final IssueManager issueManager;
  private final PermissionManager permissionManager;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final WatcherManager watcherManager;

  @Autowired
  public ChatCommandListener(
      MyteamApiClient myteamApiClient,
      MyteamChatRepository myteamChatRepository,
      UserData userData,
      MessageFormatter messageFormatter,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport IssueManager issueManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport WatcherManager watcherManager) {
    this.myteamApiClient = myteamApiClient;
    this.myteamChatRepository = myteamChatRepository;
    this.userData = userData;
    this.messageFormatter = messageFormatter;
    this.i18nResolver = i18nResolver;
    this.localeManager = localeManager;
    this.issueManager = issueManager;
    this.permissionManager = permissionManager;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.watcherManager = watcherManager;
  }

  @Subscribe
  public void onLinkIssueWithChatEvent(LinkIssueWithChatEvent linkIssueWithChatEvent)
      throws IOException, UnirestException, MyteamServerErrorException {
    log.debug("LinkIssueWithChatEvent handling started");
    ApplicationUser user = userData.getUserByMrimLogin(linkIssueWithChatEvent.getUserId());
    String chatId = linkIssueWithChatEvent.getChatId();
    String issueKey = linkIssueWithChatEvent.getIssueKey();
    Issue issue = issueManager.getIssueByKeyIgnoreCase(issueKey);
    if (issue != null) {
      if (myteamChatRepository.findChatByIssueKey(issueKey) == null) {
        myteamChatRepository.persistChat(chatId, issueKey);
        myteamApiClient.sendMessageText(
            chatId,
            i18nResolver.getText(
                localeManager.getLocaleFor(user),
                "ru.mail.jira.plugins.myteam.messageQueueProcessor.issueLinkedToChat",
                messageFormatter.createIssueLink(issue.getKey())));
      } else {
        myteamApiClient.sendMessageText(
            chatId,
            i18nResolver.getText(
                localeManager.getLocaleFor(user),
                "ru.mail.jira.plugins.myteam.messageQueueProcessor.issueLinkedToChat.error",
                messageFormatter.createIssueLink(issue.getKey())));
      }
    } else {
      myteamApiClient.sendMessageText(
          chatId,
          i18nResolver.getRawText(
              localeManager.getLocaleFor(user),
              "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.issueNotFound"));
    }
  }

  public void sendIssueViewToUser(String issueKey, ApplicationUser user, String chatId)
      throws IOException, UnirestException, MyteamServerErrorException {
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(user);
      Issue issueToShow = issueManager.getIssueByKeyIgnoreCase(issueKey);
      if (issueToShow != null) {
        if (permissionManager.hasPermission(
            ProjectPermissions.BROWSE_PROJECTS, issueToShow, user)) {
          HttpResponse<MessageResponse> response =
              myteamApiClient.sendMessageText(
                  chatId,
                  messageFormatter.createIssueSummary(issueToShow, user),
                  messageFormatter.getIssueButtons(
                      issueToShow.getKey(), user, watcherManager.isWatching(user, issueToShow)));
          if (response.getStatus() != 200
              || (response.getBody() != null && !response.getBody().isOk())) {
            log.warn(
                "sendIssueViewToUser({}, {}, {}). Text={} Response={}",
                issueKey,
                user,
                chatId,
                messageFormatter.createIssueSummary(issueToShow, user),
                response.getBody().toString());
          }
        } else {
          myteamApiClient.sendMessageText(
              chatId,
              i18nResolver.getRawText(
                  localeManager.getLocaleFor(user),
                  "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
        }
      } else {
        myteamApiClient.sendMessageText(
            chatId,
            i18nResolver.getRawText(
                localeManager.getLocaleFor(user),
                "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.issueNotFound"));
      }
    } catch (Exception e) {
      log.error("sendIssueViewToUser({}, {}, {})", issueKey, user, chatId, e);
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
    }
  }

  public void sendIssueViewToGroup(String issueKey, ApplicationUser user, String chatId)
      throws IOException, UnirestException, MyteamServerErrorException {
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(user);
      Issue issueToShow = issueManager.getIssueByKeyIgnoreCase(issueKey);
      if (issueToShow != null) {
        if (permissionManager.hasPermission(
            ProjectPermissions.BROWSE_PROJECTS, issueToShow, user)) {
          myteamApiClient.sendMessageText(
              chatId, messageFormatter.createIssueSummary(issueToShow, user), null);
        } else {
          myteamApiClient.sendMessageText(
              chatId,
              i18nResolver.getRawText(
                  localeManager.getLocaleFor(user),
                  "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
        }
      } else {
        myteamApiClient.sendMessageText(
            chatId,
            i18nResolver.getRawText(
                localeManager.getLocaleFor(user),
                "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.issueNotFound"));
      }
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
    }
  }
}
