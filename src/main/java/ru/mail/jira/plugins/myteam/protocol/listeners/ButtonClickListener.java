/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.listeners;

import static ru.mail.jira.plugins.myteam.protocol.MessageFormatter.COMMENT_LIST_PAGE_SIZE;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.Subscribe;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
import ru.mail.jira.plugins.myteam.protocol.events.buttons.*;

@Slf4j
@Component
public class ButtonClickListener {
  private final ConcurrentHashMap<String, ChatState> chatsStateMap;
  private final MyteamApiClient myteamApiClient;
  private final UserData userData;
  private final MessageFormatter messageFormatter;
  private final I18nResolver i18nResolver;
  private final LocaleManager localeManager;
  private final IssueManager issueManager;
  private final PermissionManager permissionManager;
  private final CommentManager commentManager;
  private final WatcherManager watcherManager;

  @Autowired
  public ButtonClickListener(
      ChatStateMapping chatStateMapping,
      MyteamApiClient myteamApiClient,
      UserData userData,
      MessageFormatter messageFormatter,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport IssueManager issueManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport CommentManager commentManager,
      @ComponentImport WatcherManager watcherManager) {
    this.chatsStateMap = chatStateMapping.getChatsStateMap();
    this.myteamApiClient = myteamApiClient;
    this.userData = userData;
    this.messageFormatter = messageFormatter;
    this.i18nResolver = i18nResolver;
    this.localeManager = localeManager;
    this.issueManager = issueManager;
    this.permissionManager = permissionManager;
    this.commentManager = commentManager;
    this.watcherManager = watcherManager;
  }

  @Subscribe
  public void onViewIssueButtonClick(ViewIssueClickEvent viewIssueClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    myteamApiClient.answerCallbackQuery(viewIssueClickEvent.getQueryId());
    Issue currentIssue = issueManager.getIssueByCurrentKey(viewIssueClickEvent.getIssueKey());
    ApplicationUser currentUser = userData.getUserByMrimLogin(viewIssueClickEvent.getUserId());
    if (currentUser != null && currentIssue != null) {
      if (permissionManager.hasPermission(
          ProjectPermissions.BROWSE_PROJECTS, currentIssue, currentUser)) {
        myteamApiClient.sendMessageText(
            viewIssueClickEvent.getChatId(),
            messageFormatter.createIssueSummary(currentIssue, currentUser),
            messageFormatter.getIssueButtons(
                viewIssueClickEvent.getIssueKey(),
                currentUser,
                watcherManager.isWatching(currentUser, currentIssue)));
        log.debug("ViewIssueCommand message sent...");
      } else {
        myteamApiClient.sendMessageText(
            viewIssueClickEvent.getChatId(),
            i18nResolver.getRawText(
                localeManager.getLocaleFor(currentUser),
                "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
        log.debug("ViewIssueCommand no permissions message sent...");
      }
    }
    log.debug("ViewIssueCommand execution finished...");
  }

  @Subscribe
  public void onCommentIssueButtonClick(CommentIssueClickEvent commentIssueClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    log.debug("CreateCommentCommand execution started...");
    ApplicationUser commentedUser = userData.getUserByMrimLogin(commentIssueClickEvent.getUserId());
    if (commentedUser != null) {
      String message =
          i18nResolver.getText(
              localeManager.getLocaleFor(commentedUser),
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.insertComment.message",
              commentIssueClickEvent.getIssueKey());
      myteamApiClient.answerCallbackQuery(commentIssueClickEvent.getQueryId());
      myteamApiClient.sendMessageText(
          commentIssueClickEvent.getChatId(),
          message,
          messageFormatter.getCancelButton(localeManager.getLocaleFor(commentedUser)));
      chatsStateMap.put(
          commentIssueClickEvent.getChatId(),
          ChatState.buildCommentWaitingState(commentIssueClickEvent.getIssueKey()));
    }
    log.debug("JiraMessageQueueProcessor answerCommentButtonClick queue offer finished...");
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

  @Subscribe
  public void onIssueCommentsButtonClick(ViewIssueCommentsClickEvent viewIssueCommentsClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    log.debug("ShowCommentsEvent handling started");
    JiraThreadLocalUtils.preCall();
    try {
      ApplicationUser currentUser =
          userData.getUserByMrimLogin(viewIssueCommentsClickEvent.getUserId());
      if (currentUser != null) {
        Locale locale = localeManager.getLocaleFor(currentUser);
        List<Comment> totalComments =
            commentManager.getComments(
                issueManager.getIssueByCurrentKey(viewIssueCommentsClickEvent.getIssueKey()));
        myteamApiClient.answerCallbackQuery(viewIssueCommentsClickEvent.getQueryId());
        if (totalComments.size() == 0) {
          myteamApiClient.sendMessageText(
              viewIssueCommentsClickEvent.getChatId(),
              i18nResolver.getText(
                  locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.showComments.empty"));
        } else {
          List<Comment> comments =
              totalComments.stream().limit(COMMENT_LIST_PAGE_SIZE).collect(Collectors.toList());
          myteamApiClient.sendMessageText(
              viewIssueCommentsClickEvent.getChatId(),
              messageFormatter.stringifyIssueCommentsList(
                  locale, comments, 0, totalComments.size()),
              messageFormatter.getViewCommentsButtons(
                  locale, false, totalComments.size() > COMMENT_LIST_PAGE_SIZE));

          chatsStateMap.put(
              viewIssueCommentsClickEvent.getChatId(),
              ChatState.buildIssueCommentsWatchingState(
                  viewIssueCommentsClickEvent.getIssueKey(), 0));
        }
      }
      log.debug("ShowCommentsEvent handling finished");
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  @Subscribe
  public void onNextIssueCommentsPageClickEvent(
      NextIssueCommentsPageClickEvent nextIssueCommentsPageClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    log.debug("(NextPageClickEvent handling started");
    JiraThreadLocalUtils.preCall();
    try {
      ApplicationUser currentUser =
          userData.getUserByMrimLogin(nextIssueCommentsPageClickEvent.getUserId());
      int nextPageNumber = nextIssueCommentsPageClickEvent.getCurrentPage() + 1;
      int nextPageStartIndex = nextPageNumber * COMMENT_LIST_PAGE_SIZE;
      if (currentUser != null) {
        Locale locale = localeManager.getLocaleFor(currentUser);
        List<Comment> totalComments =
            commentManager.getComments(
                issueManager.getIssueByCurrentKey(nextIssueCommentsPageClickEvent.getIssueKey()));
        List<Comment> comments =
            totalComments.stream()
                .skip(nextPageStartIndex)
                .limit(COMMENT_LIST_PAGE_SIZE)
                .collect(Collectors.toList());
        myteamApiClient.answerCallbackQuery(nextIssueCommentsPageClickEvent.getQueryId());
        myteamApiClient.editMessageText(
            nextIssueCommentsPageClickEvent.getChatId(),
            nextIssueCommentsPageClickEvent.getMsgId(),
            messageFormatter.stringifyIssueCommentsList(
                locale, comments, nextPageNumber, totalComments.size()),
            messageFormatter.getViewCommentsButtons(
                locale, true, totalComments.size() > COMMENT_LIST_PAGE_SIZE + nextPageStartIndex));
        chatsStateMap.put(
            nextIssueCommentsPageClickEvent.getChatId(),
            ChatState.buildIssueCommentsWatchingState(
                nextIssueCommentsPageClickEvent.getIssueKey(),
                nextIssueCommentsPageClickEvent.getCurrentPage() + 1));
      }
      log.debug("(NextPageClickEvent handling finished");
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  @Subscribe
  public void onPrevIssueCommentsPageClickEvent(
      PrevIssueCommentsPageClickEvent prevIssueCommentsPageClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    log.debug("NextProjectsPageClickEvent handling started");
    JiraThreadLocalUtils.preCall();
    try {
      ApplicationUser currentUser =
          userData.getUserByMrimLogin(prevIssueCommentsPageClickEvent.getUserId());
      int prevPageNumber = prevIssueCommentsPageClickEvent.getCurrentPage() - 1;
      int prevPageStartIndex = prevPageNumber * COMMENT_LIST_PAGE_SIZE;
      if (currentUser != null) {
        Locale locale = localeManager.getLocaleFor(currentUser);
        List<Comment> totalComments =
            commentManager.getComments(
                issueManager.getIssueByCurrentKey(prevIssueCommentsPageClickEvent.getIssueKey()));
        List<Comment> comments =
            totalComments.stream()
                .skip(prevPageStartIndex)
                .limit(COMMENT_LIST_PAGE_SIZE)
                .collect(Collectors.toList());

        myteamApiClient.answerCallbackQuery(prevIssueCommentsPageClickEvent.getQueryId());
        myteamApiClient.editMessageText(
            prevIssueCommentsPageClickEvent.getChatId(),
            prevIssueCommentsPageClickEvent.getMsgId(),
            messageFormatter.stringifyIssueCommentsList(
                locale, comments, prevPageNumber, totalComments.size()),
            messageFormatter.getViewCommentsButtons(
                locale, prevPageStartIndex >= COMMENT_LIST_PAGE_SIZE, true));
        chatsStateMap.put(
            prevIssueCommentsPageClickEvent.getChatId(),
            ChatState.buildIssueCommentsWatchingState(
                prevIssueCommentsPageClickEvent.getIssueKey(),
                prevIssueCommentsPageClickEvent.getCurrentPage() - 1));
      }
      log.debug("NextProjectsPageClickEvent handling finished");
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }
}
