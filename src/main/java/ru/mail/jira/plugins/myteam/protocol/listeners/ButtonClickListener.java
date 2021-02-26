/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.listeners;

import static ru.mail.jira.plugins.myteam.protocol.MessageFormatter.COMMENT_LIST_PAGE_SIZE;
import static ru.mail.jira.plugins.myteam.protocol.MessageFormatter.LIST_PAGE_SIZE;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.Subscribe;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
import ru.mail.jira.plugins.myteam.protocol.events.buttons.CommentIssueClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.NextIssueCommentsPageClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.NextIssuesPageClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.PrevIssueCommentsPageClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.PrevIssuesPageClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.SearchByJqlClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.SearchIssuesClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ShowIssueClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ViewIssueClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ViewIssueCommentsClickEvent;

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
  private final SearchService searchService;

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
      @ComponentImport SearchService searchService,
      @ComponentImport CommentManager commentManager) {
    this.chatsStateMap = chatStateMapping.getChatsStateMap();
    this.myteamApiClient = myteamApiClient;
    this.userData = userData;
    this.messageFormatter = messageFormatter;
    this.i18nResolver = i18nResolver;
    this.localeManager = localeManager;
    this.issueManager = issueManager;
    this.permissionManager = permissionManager;
    this.commentManager = commentManager;
    this.searchService = searchService;
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
            messageFormatter.getIssueButtons(viewIssueClickEvent.getIssueKey(), currentUser));
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
  public void onSearchIssuesClickEvent(SearchIssuesClickEvent searchIssuesClickEvent)
      throws IOException, UnirestException, SearchException, MyteamServerErrorException {
    log.debug("ShowIssuesFilterResultsEvent handling started");
    JiraThreadLocalUtils.preCall();
    try {
      ApplicationUser currentUser = userData.getUserByMrimLogin(searchIssuesClickEvent.getUserId());
      if (currentUser != null) {
        Locale locale = localeManager.getLocaleFor(currentUser);
        SearchService.ParseResult parseResult =
            searchService.parseQuery(currentUser, searchIssuesClickEvent.getJqlClause());
        if (parseResult.isValid()) {
          Query jqlQuery = parseResult.getQuery();
          Query sanitizedJql = searchService.sanitiseSearchQuery(currentUser, jqlQuery);
          PagerFilter<Issue> pagerFilter = new PagerFilter<>(0, LIST_PAGE_SIZE);
          SearchResults<Issue> searchResults =
              searchService.search(currentUser, sanitizedJql, pagerFilter);
          int totalResultsSize = searchResults.getTotal();
          myteamApiClient.answerCallbackQuery(searchIssuesClickEvent.getQueryId());
          if (totalResultsSize == 0) {
            myteamApiClient.sendMessageText(
                searchIssuesClickEvent.getChatId(),
                i18nResolver.getText(
                    locale,
                    "ru.mail.jira.plugins.myteam.myteamEventsListener.searchIssues.emptyResult"));
          } else {
            myteamApiClient.sendMessageText(
                searchIssuesClickEvent.getChatId(),
                messageFormatter.stringifyIssueList(
                    locale, searchResults.getResults(), 0, totalResultsSize),
                messageFormatter.getIssueListButtons(
                    locale, false, totalResultsSize > LIST_PAGE_SIZE));

            chatsStateMap.put(
                searchIssuesClickEvent.getChatId(),
                ChatState.buildIssueSearchResultsWatchingState(sanitizedJql, 0));
          }
        } else {
          myteamApiClient.answerCallbackQuery(searchIssuesClickEvent.getQueryId());
          myteamApiClient.sendMessageText(
              searchIssuesClickEvent.getChatId(),
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.searchIssues.jqlParseError.text"));
        }
      }
      log.debug("ShowIssuesFilterResultsEvent handling finished");
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  @Subscribe
  public void onNextIssuesPageClickEvent(NextIssuesPageClickEvent nextIssuesPageClickEvent)
      throws SearchException, UnirestException, IOException, MyteamServerErrorException {
    log.debug("(NextPageClickEvent handling started");
    JiraThreadLocalUtils.preCall();
    try {
      ApplicationUser currentUser =
          userData.getUserByMrimLogin(nextIssuesPageClickEvent.getUserId());
      int nextPageStartIndex = (nextIssuesPageClickEvent.getCurrentPage() + 1) * LIST_PAGE_SIZE;
      if (currentUser != null) {
        Locale locale = localeManager.getLocaleFor(currentUser);
        PagerFilter<Issue> pagerFilter = new PagerFilter<>(nextPageStartIndex, LIST_PAGE_SIZE);
        SearchResults<Issue> searchResults =
            searchService.search(
                currentUser, nextIssuesPageClickEvent.getCurrentJqlQueryClause(), pagerFilter);
        myteamApiClient.answerCallbackQuery(nextIssuesPageClickEvent.getQueryId());
        myteamApiClient.editMessageText(
            nextIssuesPageClickEvent.getChatId(),
            nextIssuesPageClickEvent.getMsgId(),
            messageFormatter.stringifyIssueList(
                locale,
                searchResults.getResults(),
                nextIssuesPageClickEvent.getCurrentPage() + 1,
                searchResults.getTotal()),
            messageFormatter.getIssueListButtons(
                locale, true, searchResults.getTotal() > LIST_PAGE_SIZE + nextPageStartIndex));
        chatsStateMap.put(
            nextIssuesPageClickEvent.getChatId(),
            ChatState.buildIssueSearchResultsWatchingState(
                nextIssuesPageClickEvent.getCurrentJqlQueryClause(),
                nextIssuesPageClickEvent.getCurrentPage() + 1));
      }
      log.debug("(NextPageClickEvent handling finished");
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  @Subscribe
  public void onPrevIssuesPageClickEvent(PrevIssuesPageClickEvent prevIssuesPageClickEvent)
      throws UnirestException, IOException, SearchException, MyteamServerErrorException {
    log.debug("NextProjectsPageClickEvent handling started");
    JiraThreadLocalUtils.preCall();
    try {
      ApplicationUser currentUser =
          userData.getUserByMrimLogin(prevIssuesPageClickEvent.getUserId());
      int prevPageStartIndex = (prevIssuesPageClickEvent.getCurrentPage() - 1) * LIST_PAGE_SIZE;
      if (currentUser != null) {
        Locale locale = localeManager.getLocaleFor(currentUser);
        PagerFilter<Issue> pagerFilter = new PagerFilter<>(prevPageStartIndex, LIST_PAGE_SIZE);
        SearchResults<Issue> searchResults =
            searchService.search(
                currentUser, prevIssuesPageClickEvent.getCurrentJqlQueryClause(), pagerFilter);
        myteamApiClient.answerCallbackQuery(prevIssuesPageClickEvent.getQueryId());
        myteamApiClient.editMessageText(
            prevIssuesPageClickEvent.getChatId(),
            prevIssuesPageClickEvent.getMsgId(),
            messageFormatter.stringifyIssueList(
                locale,
                searchResults.getResults(),
                prevIssuesPageClickEvent.getCurrentPage() - 1,
                searchResults.getTotal()),
            messageFormatter.getIssueListButtons(
                locale, prevPageStartIndex >= LIST_PAGE_SIZE, true));
        chatsStateMap.put(
            prevIssuesPageClickEvent.getChatId(),
            ChatState.buildIssueSearchResultsWatchingState(
                prevIssuesPageClickEvent.getCurrentJqlQueryClause(),
                prevIssuesPageClickEvent.getCurrentPage() - 1));
      }
      log.debug("NextProjectsPageClickEvent handling finished");
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  @Subscribe
  public void onSearchByJqlClickEvent(SearchByJqlClickEvent searchByJqlClickEvent)
      throws UnirestException, IOException, MyteamServerErrorException {
    log.debug("SearchByJqlClickEvent handling started");
    ApplicationUser currentUser = userData.getUserByMrimLogin(searchByJqlClickEvent.getUserId());
    String chatId = searchByJqlClickEvent.getChatId();
    if (currentUser != null) {
      Locale locale = localeManager.getLocaleFor(currentUser);
      myteamApiClient.answerCallbackQuery(searchByJqlClickEvent.getQueryId());
      myteamApiClient.sendMessageText(
          chatId,
          i18nResolver.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.myteamEventsListener.searchByJqlClauseButton.insertJqlClause.message"),
          messageFormatter.buildButtonsWithCancel(
              null,
              i18nResolver.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.mrimsenderEventListener.cancelButton.text")));
      chatsStateMap.put(chatId, ChatState.jqlClauseWaitingState);
    }
    log.debug("SearchByJqlClickEvent handling finished");
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
