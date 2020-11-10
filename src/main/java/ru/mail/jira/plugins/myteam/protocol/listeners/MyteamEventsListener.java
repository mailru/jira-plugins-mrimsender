/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol.listeners;

import static ru.mail.jira.plugins.myteam.protocol.MessageFormatter.LIST_PAGE_SIZE;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.protocol.ChatState;
import ru.mail.jira.plugins.myteam.protocol.ChatStateMapping;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.Event;
import ru.mail.jira.plugins.myteam.protocol.events.IssueKeyMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.protocol.events.NewCommentMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.NewIssueFieldValueMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.SearchIssuesEvent;
import ru.mail.jira.plugins.myteam.protocol.events.SelectedIssueTypeMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.SelectedProjectMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.ShowDefaultMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.ShowHelpEvent;
import ru.mail.jira.plugins.myteam.protocol.events.ShowIssueEvent;
import ru.mail.jira.plugins.myteam.protocol.events.ShowMenuEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.CancelClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.CommentIssueClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.CreateIssueClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.IssueTypeButtonClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.NewIssueFieldValueButtonClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.NextIssueCommentsPageClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.NextIssuesPageClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.NextProjectsPageClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.PrevIssueCommentsPageClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.PrevIssuesPageClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.PrevProjectsPageClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.SearchByJqlClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.SearchIssuesClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ShowIssueClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ViewIssueClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ViewIssueCommentsClickEvent;

@Slf4j
@Component
public class MyteamEventsListener {
  private static final String THREAD_NAME_PREFIX = "icq-events-listener-thread-pool";
  private static final String CHAT_COMMAND_PREFIX = "/";

  private final ConcurrentHashMap<String, ChatState> chatsStateMap;
  private final ExecutorService executorService =
      Executors.newFixedThreadPool(
          2, new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_PREFIX).build());
  private final AsyncEventBus asyncEventBus;
  private final MyteamApiClient myteamApiClient;
  private final UserData userData;
  private final MessageFormatter messageFormatter;
  private final I18nResolver i18nResolver;
  private final LocaleManager localeManager;
  private final IssueManager issueManager;
  private final PermissionManager permissionManager;
  private final CommentManager commentManager;
  private final SearchService searchService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final String JIRA_BASE_URL;

  @Autowired
  public MyteamEventsListener(
      ChatStateMapping chatStateMapping,
      MyteamApiClient myteamApiClient,
      UserData userData,
      MessageFormatter messageFormatter,
      ChatCommandListener chatCommandListener,
      ButtonClickListener buttonClickListener,
      CreateIssueEventsListener createIssueEventsListener,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport IssueManager issueManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport CommentManager commentManager,
      @ComponentImport SearchService searchService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport ApplicationProperties applicationProperties) {
    this.chatsStateMap = chatStateMapping.getChatsStateMap();
    this.asyncEventBus =
        new AsyncEventBus(
            executorService,
            (exception, context) ->
                log.error(
                    String.format(
                        "Exception occurred in subscriber = %s",
                        context.getSubscriber().toString()),
                    exception));
    this.asyncEventBus.register(this);
    this.asyncEventBus.register(chatCommandListener);
    this.asyncEventBus.register(buttonClickListener);
    this.asyncEventBus.register(createIssueEventsListener);
    this.myteamApiClient = myteamApiClient;
    this.userData = userData;
    this.messageFormatter = messageFormatter;
    this.i18nResolver = i18nResolver;
    this.localeManager = localeManager;
    this.issueManager = issueManager;
    this.permissionManager = permissionManager;
    this.commentManager = commentManager;
    this.searchService = searchService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.JIRA_BASE_URL = applicationProperties.getString(APKeys.JIRA_BASEURL);
  }

  public void publishEvent(Event event) {
    asyncEventBus.post(event);
  }

  @Subscribe
  public void handleNewMessageEvent(ChatMessageEvent chatMessageEvent) {
    String chatId = chatMessageEvent.getChatId();
    boolean isGroupChatEvent = chatMessageEvent.getChatType() == ChatType.GROUP;

    // if chat is in some state then use our state processing logic
    if (!isGroupChatEvent && chatsStateMap.containsKey(chatId)) {
      ChatState chatState = chatsStateMap.remove(chatId);
      if (chatState.isWaitingForComment()) {
        asyncEventBus.post(new NewCommentMessageEvent(chatMessageEvent, chatState.getIssueKey()));
        return;
      }
      if (chatState.isWaitingForIssueKey()) {
        asyncEventBus.post(new IssueKeyMessageEvent(chatMessageEvent, JIRA_BASE_URL));
        return;
      }
      if (chatState.isWaitingForJqlClause()) {
        asyncEventBus.post(new SearchIssuesEvent(chatMessageEvent));
        return;
      }
      if (chatState.isWaitingForProjectSelect()) {
        asyncEventBus.post(
            new SelectedProjectMessageEvent(chatMessageEvent, chatState.getIssueCreationDto()));
        return;
      }
      if (chatState.isWaitingForIssueTypeSelect()) {
        asyncEventBus.post(
            new SelectedIssueTypeMessageEvent(chatMessageEvent, chatState.getIssueCreationDto()));
        return;
      }
      if (chatState.isNewIssueFieldsFillingState()) {
        asyncEventBus.post(
            new NewIssueFieldValueMessageEvent(
                chatMessageEvent,
                chatState.getIssueCreationDto(),
                chatState.getCurrentFillingFieldNum()));
        return;
      }
    }

    // if chat isn't in some state then just process new message
    String message = chatMessageEvent.getMessage();

    if (message != null && message.startsWith(CHAT_COMMAND_PREFIX)) {
      String command = StringUtils.substringAfter(message, CHAT_COMMAND_PREFIX).toLowerCase();
      if (command.startsWith("help")) {
        asyncEventBus.post(new ShowHelpEvent(chatMessageEvent));
      }
      if (command.startsWith("menu") && !isGroupChatEvent) {
        asyncEventBus.post(new ShowMenuEvent(chatMessageEvent));
      }
      if (command.startsWith("issue")) {
        asyncEventBus.post(new ShowIssueEvent(chatMessageEvent, JIRA_BASE_URL));
      }
    } else if (!isGroupChatEvent && (message != null || chatMessageEvent.isHasForwards())) {
      asyncEventBus.post(new ShowDefaultMessageEvent(chatMessageEvent));
    }
  }

  @Subscribe
  public void handleButtonClickEvent(ButtonClickEvent buttonClickEvent)
      throws UnirestException, IOException {
    String buttonPrefix = StringUtils.substringBefore(buttonClickEvent.getCallbackData(), "-");
    String chatId = buttonClickEvent.getChatId();
    boolean isGroupChatEvent = buttonClickEvent.getChatType() == ChatType.GROUP;

    // if chat is in some state then use our state processing logic
    if (!isGroupChatEvent && chatsStateMap.containsKey(chatId)) {
      ChatState chatState = chatsStateMap.remove(chatId);
      if (chatState.isIssueSearchResultsShowing()) {
        if (buttonPrefix.equals("nextIssueListPage")) {
          asyncEventBus.post(
              new NextIssuesPageClickEvent(
                  buttonClickEvent,
                  chatState.getCurrentSelectListPage(),
                  chatState.getCurrentSearchJqlClause()));
          return;
        }
        if (buttonPrefix.equals("prevIssueListPage")) {
          asyncEventBus.post(
              new PrevIssuesPageClickEvent(
                  buttonClickEvent,
                  chatState.getCurrentSelectListPage(),
                  chatState.getCurrentSearchJqlClause()));
          return;
        }
      }
      if (chatState.isIssueCommentsShowing()) {
        if (buttonPrefix.equals("nextIssueCommentsListPage")) {
          asyncEventBus.post(
              new NextIssueCommentsPageClickEvent(
                  buttonClickEvent, chatState.getCurrentSelectListPage(), chatState.getIssueKey()));
          return;
        }
        if (buttonPrefix.equals("prevIssueCommentsListPage")) {
          asyncEventBus.post(
              new PrevIssueCommentsPageClickEvent(
                  buttonClickEvent, chatState.getCurrentSelectListPage(), chatState.getIssueKey()));
          return;
        }
      }
      if (chatState.isWaitingForProjectSelect()) {
        if (buttonPrefix.equals("nextProjectListPage")) {
          asyncEventBus.post(
              new NextProjectsPageClickEvent(
                  buttonClickEvent,
                  chatState.getCurrentSelectListPage(),
                  chatState.getIssueCreationDto()));
          return;
        }
        if (buttonPrefix.equals("prevProjectListPage")) {
          asyncEventBus.post(
              new PrevProjectsPageClickEvent(
                  buttonClickEvent,
                  chatState.getCurrentSelectListPage(),
                  chatState.getIssueCreationDto()));
          return;
        }
      }
      if (chatState.isWaitingForIssueTypeSelect()) {
        if (buttonPrefix.equals("selectIssueType")) {
          asyncEventBus.post(
              new IssueTypeButtonClickEvent(buttonClickEvent, chatState.getIssueCreationDto()));
          return;
        }
      }
      if (chatState.isWaitingForNewIssueButtonFillingState()) {
        if (buttonPrefix.equals("selectIssueButtonValue")) {
          asyncEventBus.post(
              new NewIssueFieldValueButtonClickEvent(
                  buttonClickEvent,
                  chatState.getIssueCreationDto(),
                  chatState.getCurrentFillingFieldNum()));
          return;
        }
      }
      if (chatState.isWaiting() && buttonPrefix.equals("cancel")) {
        asyncEventBus.post(new CancelClickEvent(buttonClickEvent));
        return;
      }
    }

    // if chat isn't in some state then just process new command
    switch (buttonPrefix) {
      case "view":
        asyncEventBus.post(new ViewIssueClickEvent(buttonClickEvent));
        break;
      case "comment":
        asyncEventBus.post(new CommentIssueClickEvent(buttonClickEvent));
        break;
      case "showComments":
        asyncEventBus.post(new ViewIssueCommentsClickEvent(buttonClickEvent));
        break;
      case "showIssue":
        asyncEventBus.post(new ShowIssueClickEvent(buttonClickEvent));
        break;
      case "activeIssuesAssigned":
        asyncEventBus.post(
            new SearchIssuesClickEvent(
                buttonClickEvent,
                "assignee = currentUser() AND resolution = Unresolved ORDER BY updated"));
        break;
      case "activeIssuesWatching":
        asyncEventBus.post(
            new SearchIssuesClickEvent(
                buttonClickEvent,
                "watcher = currentUser() AND resolution = Unresolved ORDER BY updated"));
        break;
      case "activeIssuesCreated":
        asyncEventBus.post(
            new SearchIssuesClickEvent(
                buttonClickEvent,
                "reporter = currentUser() AND resolution = Unresolved ORDER BY updated"));
        break;
      case "searchByJql":
        asyncEventBus.post(new SearchByJqlClickEvent(buttonClickEvent));
        break;
      case "createIssue":
        asyncEventBus.post(new CreateIssueClickEvent(buttonClickEvent));
        break;
      case "showMenu":
        // answer button click here because ShowMenuEvent is originally MessageEvent =/
        myteamApiClient.answerCallbackQuery(buttonClickEvent.getQueryId());
        asyncEventBus.post(new ShowMenuEvent(buttonClickEvent));
        break;
      default:
        // fix infinite spinners situations for not recognized button clicks
        // for example next or prev button click when chat state was cleared
        myteamApiClient.answerCallbackQuery(buttonClickEvent.getQueryId());
        break;
    }
  }

  @Subscribe
  public void handleJiraNotifyEvent(JiraNotifyEvent jiraNotifyEvent) throws Exception {
    myteamApiClient.sendMessageText(
        jiraNotifyEvent.getChatId(), jiraNotifyEvent.getMessage(), jiraNotifyEvent.getButtons());
  }

  @Subscribe
  public void handleNewCommentMessageEvent(NewCommentMessageEvent newCommentMessageEvent)
      throws IOException, UnirestException {
    JiraThreadLocalUtils.preCall();
    try {
      log.debug("CreateCommentCommand execution started...");
      ApplicationUser commentedUser =
          userData.getUserByMrimLogin(newCommentMessageEvent.getUserId());
      Issue commentedIssue =
          issueManager.getIssueByCurrentKey(newCommentMessageEvent.getCommentingIssueKey());
      if (commentedUser != null && commentedIssue != null) {
        if (permissionManager.hasPermission(
            ProjectPermissions.ADD_COMMENTS, commentedIssue, commentedUser)) {
          commentManager.create(
              commentedIssue, commentedUser, newCommentMessageEvent.getMessage(), true);
          log.debug("CreateCommentCommand comment created...");
          myteamApiClient.sendMessageText(
              newCommentMessageEvent.getChatId(),
              i18nResolver.getText(
                  localeManager.getLocaleFor(commentedUser),
                  "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.commentCreated"));
          log.debug("CreateCommentCommand new comment created message sent...");
        } else {
          log.debug("CreateCommentCommand permissions violation occurred...");
          myteamApiClient.sendMessageText(
              newCommentMessageEvent.getChatId(),
              i18nResolver.getText(
                  localeManager.getLocaleFor(commentedUser),
                  "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.noPermissions"));
          log.debug("CreateCommentCommand not enough permissions message sent...");
        }
      }
      log.debug("CreateCommentCommand execution finished...");
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  @Subscribe
  public void handleNewIssueKeyMessageEvent(IssueKeyMessageEvent issueKeyMessageEvent)
      throws IOException, UnirestException {
    log.debug("NewIssueKeyMessageEvent handling started");
    ApplicationUser currentUser = userData.getUserByMrimLogin(issueKeyMessageEvent.getUserId());
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(currentUser);
      Issue currentIssue = issueManager.getIssueByKeyIgnoreCase(issueKeyMessageEvent.getIssueKey());
      if (currentUser != null && currentIssue != null) {
        if (permissionManager.hasPermission(
            ProjectPermissions.BROWSE_PROJECTS, currentIssue, currentUser)) {
          myteamApiClient.sendMessageText(
              issueKeyMessageEvent.getChatId(),
              messageFormatter.createIssueSummary(currentIssue, currentUser),
              messageFormatter.getIssueButtons(currentIssue.getKey(), currentUser));
          log.debug("ViewIssueCommand message sent...");
        } else {
          myteamApiClient.sendMessageText(
              issueKeyMessageEvent.getChatId(),
              i18nResolver.getRawText(
                  localeManager.getLocaleFor(currentUser),
                  "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
          log.debug("ViewIssueCommand no permissions message sent...");
        }
      } else if (currentUser != null) {
        myteamApiClient.sendMessageText(
            issueKeyMessageEvent.getChatId(),
            i18nResolver.getRawText(
                localeManager.getLocaleFor(currentUser),
                "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.issueNotFound"));
      }
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
    }
    log.debug("NewIssueKeyMessageEvent handling finished");
  }

  @Subscribe
  public void onSearchIssuesEvent(SearchIssuesEvent searchIssuesEvent)
      throws IOException, UnirestException, SearchException {
    log.debug("ShowIssuesFilterResultsEvent handling started");
    JiraThreadLocalUtils.preCall();
    try {
      ApplicationUser currentUser = userData.getUserByMrimLogin(searchIssuesEvent.getUserId());
      if (currentUser != null) {
        Locale locale = localeManager.getLocaleFor(currentUser);
        SearchService.ParseResult parseResult =
            searchService.parseQuery(currentUser, searchIssuesEvent.getJqlClause());
        if (parseResult.isValid()) {
          Query jqlQuery = parseResult.getQuery();
          Query sanitizedJql = searchService.sanitiseSearchQuery(currentUser, jqlQuery);
          MessageSet messageSet = searchService.validateQuery(currentUser, sanitizedJql);
          if (!messageSet.hasAnyErrors()) {
            PagerFilter<Issue> pagerFilter = new PagerFilter<>(0, LIST_PAGE_SIZE);
            SearchResults<Issue> searchResults =
                searchService.search(currentUser, sanitizedJql, pagerFilter);
            int totalResultsSize = searchResults.getTotal();
            if (totalResultsSize == 0)
              myteamApiClient.sendMessageText(
                  searchIssuesEvent.getChatId(),
                  i18nResolver.getText(
                      locale,
                      "ru.mail.jira.plugins.myteam.myteamEventsListener.searchIssues.emptyResult"));
            else {
              myteamApiClient.sendMessageText(
                  searchIssuesEvent.getChatId(),
                  messageFormatter.stringifyIssueList(
                      locale, searchResults.getResults(), 0, totalResultsSize),
                  messageFormatter.getIssueListButtons(
                      locale, false, totalResultsSize > LIST_PAGE_SIZE));

              chatsStateMap.put(
                  searchIssuesEvent.getChatId(),
                  ChatState.buildIssueSearchResultsWatchingState(sanitizedJql, 0));
            }
          } else {
            myteamApiClient.sendMessageText(
                searchIssuesEvent.getChatId(),
                String.join(
                    "\n",
                    i18nResolver.getRawText(
                        locale,
                        "ru.mail.jira.plugins.myteam.myteamEventsListener.searchIssues.jqlParseError.text"),
                    messageFormatter.stringifyJqlClauseErrorsMap(messageSet, locale)));
          }
        } else {
          myteamApiClient.sendMessageText(
              searchIssuesEvent.getChatId(),
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
}
