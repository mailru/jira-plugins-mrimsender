package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.ChatType;
import ru.mail.jira.plugins.mrimsender.protocol.events.*;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class IcqEventsListener {
    private static final String THREAD_NAME_PREFIX = "icq-events-listener-thread-pool";
    private static final String CHAT_COMMAND_PREFIX = "/";
    private static final int LIST_PAGE_SIZE = 15;

    private final ConcurrentHashMap<String, ChatState> chatsStateMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_PREFIX).build());
    private final AsyncEventBus asyncEventBus;
    private final IcqApiClient icqApiClient;
    private final UserData userData;
    private final MessageFormatter messageFormatter;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final IssueManager issueManager;
    private final PermissionManager permissionManager;
    private final CommentManager commentManager;
    private final SearchService searchService;

    public IcqEventsListener(IcqApiClient icqApiClient,
                             UserData userData,
                             MessageFormatter messageFormatter,
                             LocaleManager localeManager,
                             I18nResolver i18nResolver,
                             IssueManager issueManager,
                             PermissionManager permissionManager,
                             CommentManager commentManager,
                             SearchService searchService) {
        this.asyncEventBus = new AsyncEventBus(executorService, (exception, context) -> log.error(String.format("Exception occurred in subscriber = %s", context.getSubscriber().toString()), exception));
        this.asyncEventBus.register(this);
        this.icqApiClient = icqApiClient;
        this.userData = userData;
        this.messageFormatter = messageFormatter;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.commentManager = commentManager;
        this.searchService = searchService;
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
                asyncEventBus.post(new IssueKeyMessageEvent(chatMessageEvent));
                return;
            }
            if (chatState.isWaitingForJqlClause()) {
                asyncEventBus.post(new SearchIssuesEvent(chatMessageEvent));
                return;
            }
        }

        // if chat isn't in some state then just process new message
        String message = chatMessageEvent.getMessage();
        if (message != null) {
            if (message.startsWith(CHAT_COMMAND_PREFIX)) {
                String command = StringUtils.substringAfter(message, CHAT_COMMAND_PREFIX).toLowerCase();
                if (command.startsWith("help")) {
                    asyncEventBus.post(new ShowHelpEvent(chatMessageEvent));
                }
                if (command.startsWith("menu") && !isGroupChatEvent) {
                    asyncEventBus.post(new ShowMenuEvent(chatMessageEvent));
                }
                if (command.startsWith("issue")) {
                    asyncEventBus.post(new ShowIssueEvent(chatMessageEvent));
                }
            } else if (!isGroupChatEvent) {
                asyncEventBus.post(new ShowDefaultMessageEvent(chatMessageEvent));
            }
        }

    }

    @Subscribe
    public void handleButtonClickEvent(ButtonClickEvent buttonClickEvent) {
        String buttonPrefix = StringUtils.substringBefore(buttonClickEvent.getCallbackData(), "-");
        String chatId = buttonClickEvent.getChatId();
        boolean isGroupChatEvent = buttonClickEvent.getChatType() == ChatType.GROUP;

        // if chat is in some state then use our state processing logic
        if (!isGroupChatEvent && chatsStateMap.containsKey(chatId)) {
            ChatState chatState = chatsStateMap.remove(chatId);
            if (chatState.isSearchResultsShowing() && buttonPrefix.equals("nextListPage")) {
                asyncEventBus.post(new NextPageClickEvent(buttonClickEvent, chatState.getCurrentSearchResultsPage(), chatState.getCurrentSearchJqlClause()));
                return;
            }
            if (chatState.isSearchResultsShowing() && buttonPrefix.equals("prevListPage")) {
                asyncEventBus.post(new PrevPageClickEvent(buttonClickEvent, chatState.getCurrentSearchResultsPage(), chatState.getCurrentSearchJqlClause()));
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
            case "cancel":
                asyncEventBus.post(new CancelClickEvent(buttonClickEvent));
                break;
            case "showIssue":
                asyncEventBus.post(new ShowIssueClickEvent(buttonClickEvent));
                break;
            case "activeIssuesAssigned":
                asyncEventBus.post(new SearchIssuesClickEvent(buttonClickEvent, "assignee = currentUser() AND resolution = Unresolved ORDER BY updated"));
                break;
            case "activeIssuesWatching":
                asyncEventBus.post(new SearchIssuesClickEvent(buttonClickEvent, "watcher = currentUser() AND resolution = Unresolved ORDER BY updated"));
                break;
            case "activeIssuesCreated":
                asyncEventBus.post(new SearchIssuesClickEvent(buttonClickEvent, "reporter = currentUser() AND resolution = Unresolved ORDER BY updated"));
                break;
            case "searchByJql":
                asyncEventBus.post(new SearchByJqlClickEvent(buttonClickEvent));
                break;
            default:
                break;
        }

        // if chat isn't in some state then just process new command
        switch (buttonPrefix) {
            case "view":
                asyncEventBus.post(new ViewIssueClickEvent(chatButtonClickEvent));
                break;
            case "comment":
                asyncEventBus.post(new CommentIssueClickEvent(chatButtonClickEvent));
                break;
            case "cancel":
                asyncEventBus.post(new CancelClickEvent(chatButtonClickEvent));
                break;
            case "showIssue":
                asyncEventBus.post(new ShowIssueClickEvent(chatButtonClickEvent));
                break;
            case "activeIssuesAssigned":
                asyncEventBus.post(new SearchIssuesClickEvent(chatButtonClickEvent, "assignee = currentUser() AND resolution = Unresolved ORDER BY updated"));
                break;
            case "activeIssuesWatching":
                asyncEventBus.post(new SearchIssuesClickEvent(chatButtonClickEvent, "watcher = currentUser() AND resolution = Unresolved ORDER BY updated"));
                break;
            case "activeIssuesCreated":
                asyncEventBus.post(new SearchIssuesClickEvent(chatButtonClickEvent, "reporter = currentUser() AND resolution = Unresolved ORDER BY updated"));
                break;
            case "searchByJql":
                asyncEventBus.post(new SearchByJqlClickEvent(chatButtonClickEvent));
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void handleJiraNotifyEvent(JiraNotifyEvent jiraNotifyEvent) throws Exception {
        icqApiClient.sendMessageText(jiraNotifyEvent.getChatId(), jiraNotifyEvent.getMessage(), jiraNotifyEvent.getButtons());
    }

    @Subscribe
    public void onViewIssueButtonClick(ViewIssueClickEvent viewIssueClickEvent) throws UnirestException, IOException {
        icqApiClient.answerCallbackQuery(viewIssueClickEvent.getQueryId());
        Issue currentIssue = issueManager.getIssueByCurrentKey(viewIssueClickEvent.getIssueKey());
        ApplicationUser currentUser = userData.getUserByMrimLogin(viewIssueClickEvent.getUserId());
        if (currentUser != null && currentIssue != null) {
            if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, currentIssue, currentUser)) {
                icqApiClient.sendMessageText(viewIssueClickEvent.getChatId(), messageFormatter.createIssueSummary(currentIssue, currentUser), messageFormatter.getIssueButtons(viewIssueClickEvent.getIssueKey(), currentUser));
                log.debug("ViewIssueCommand message sent...");
            } else {
                icqApiClient.sendMessageText(viewIssueClickEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"));
                log.debug("ViewIssueCommand no permissions message sent...");
            }
        }
        log.debug("ViewIssueCommand execution finished...");
    }

    @Subscribe
    public void onCommentIssueButtonClick(CommentIssueClickEvent commentIssueClickEvent) throws UnirestException, IOException {
        log.debug("CreateCommentCommand execution started...");
        ApplicationUser commentedUser = userData.getUserByMrimLogin(commentIssueClickEvent.getUserId());
        if (commentedUser != null) {
            String message = i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.insertComment.message", commentIssueClickEvent.getIssueKey());
            icqApiClient.answerCallbackQuery(commentIssueClickEvent.getQueryId());
            icqApiClient.sendMessageText(commentIssueClickEvent.getChatId(), message, messageFormatter.getCancelButton(commentedUser));
            chatsStateMap.put(commentIssueClickEvent.getChatId(), ChatState.buildCommentWaitingState(commentIssueClickEvent.getIssueKey()));
        }
        log.debug("JiraMessageQueueProcessor answerCommentButtonClick queue offer finished...");
    }

    @Subscribe
    public void handleNewCommentMessageEvent(NewCommentMessageEvent newCommentMessageEvent) throws IOException, UnirestException {
        JiraThreadLocalUtils.preCall();
        try {
            log.debug("CreateCommentCommand execution started...");
            ApplicationUser commentedUser = userData.getUserByMrimLogin(newCommentMessageEvent.getUserId());
            Issue commentedIssue = issueManager.getIssueByCurrentKey(newCommentMessageEvent.getCommentingIssueKey());
            if (commentedUser != null && commentedIssue != null) {
                if (permissionManager.hasPermission(ProjectPermissions.ADD_COMMENTS, commentedIssue, commentedUser)) {
                    commentManager.create(commentedIssue, commentedUser, newCommentMessageEvent.getMessage(), true);
                    log.debug("CreateCommentCommand comment created...");
                    icqApiClient.sendMessageText(newCommentMessageEvent.getChatId(), i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.commentCreated"));
                    log.debug("CreateCommentCommand new comment created message sent...");
                } else {
                    log.debug("CreateCommentCommand permissions violation occurred...");
                    icqApiClient.sendMessageText(newCommentMessageEvent.getChatId(), i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.noPermissions"));
                    log.debug("CreateCommentCommand not enough permissions message sent...");
                }
            }
            log.debug("CreateCommentCommand execution finished...");
        } finally {
            JiraThreadLocalUtils.postCall();
        }
    }

    @Subscribe
    public void onCancelButtonClick(CancelClickEvent cancelClickEvent) throws UnirestException {
        log.debug("CancelCommand execution started...");
        String message = i18nResolver.getRawText(localeManager.getLocaleFor(userData.getUserByMrimLogin(cancelClickEvent.getUserId())), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.cancelComment.message");
        icqApiClient.answerCallbackQuery(cancelClickEvent.getQueryId(), message, false, null);
        chatsStateMap.remove(cancelClickEvent.getChatId());
        log.debug("CancelCommand execution finished...");
    }

    @Subscribe
    public void onShowMenuEvent(ShowMenuEvent showMenuEvent) throws IOException, UnirestException {
        log.debug("ShowDefaultMenuEvent handling started...");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showMenuEvent.getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            icqApiClient.sendMessageText(showMenuEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.mainMenu.text"), messageFormatter.getMenuButtons(locale));
        }
        log.debug("JiraMessageQueueProcessor showDefaultMenu finished...");
    }

    @Subscribe
    public void onShowHelpEvent(ShowHelpEvent showHelpEvent) throws IOException, UnirestException {
        log.debug("ShowHelpEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showHelpEvent.getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            if (showHelpEvent.getChatType() == ChatType.GROUP)
                icqApiClient.sendMessageText(showHelpEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.groupChat.helpMessage.text"));
            else
                icqApiClient.sendMessageText(showHelpEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.helpMessage.text"));
        }
        log.debug("ShowHelpEvent handling finished");
    }

    @Subscribe
    public void onShowIssueButtonClick(ShowIssueClickEvent showIssueClickEvent) throws IOException, UnirestException {
        log.debug("OnSearchIssueButtonClick event handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showIssueClickEvent.getUserId());
        if (currentUser != null) {
            String message = i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.searchButton.insertIssueKey.message");
            icqApiClient.answerCallbackQuery(showIssueClickEvent.getQueryId());
            icqApiClient.sendMessageText(showIssueClickEvent.getChatId(), message, messageFormatter.getCancelButton(currentUser));
            chatsStateMap.put(showIssueClickEvent.getChatId(), ChatState.issueKeyWaitingState());
        }
        log.debug("OnSearchIssueButtonClick event handling finished");
    }

    @Subscribe
    public void handleNewIssueKeyMessageEvent(IssueKeyMessageEvent issueKeyMessageEvent) throws IOException, UnirestException {
        log.debug("NewIssueKeyMessageEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(issueKeyMessageEvent.getUserId());
        Issue currentIssue = issueManager.getIssueByCurrentKey(issueKeyMessageEvent.getIssueKey());
        if (currentUser != null && currentIssue != null) {
            if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, currentIssue, currentUser)) {
                icqApiClient.sendMessageText(issueKeyMessageEvent.getChatId(), messageFormatter.createIssueSummary(currentIssue, currentUser), messageFormatter.getIssueButtons(currentIssue.getKey(), currentUser));
                log.debug("ViewIssueCommand message sent...");
            } else {
                icqApiClient.sendMessageText(issueKeyMessageEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"));
                log.debug("ViewIssueCommand no permissions message sent...");
            }
        } else if (currentUser != null) {
            icqApiClient.sendMessageText(issueKeyMessageEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.icqEventsListener.newIssueKeyMessage.error.issueNotFound"));
        }
        log.debug("NewIssueKeyMessageEvent handling finished");
    }

    @Subscribe
    public void onShowIssueEvent(ShowIssueEvent showIssueEvent) throws IOException, UnirestException {
        log.debug("ShowIssueEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showIssueEvent.getUserId());
        Issue issueToShow = issueManager.getIssueByCurrentKey(showIssueEvent.getIssueKey());
        if (currentUser != null && issueToShow != null) {
            if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issueToShow, currentUser)) {
                icqApiClient.sendMessageText(showIssueEvent.getChatId(), messageFormatter.createIssueSummary(issueToShow, currentUser), messageFormatter.getIssueButtons(issueToShow.getKey(), currentUser));
            } else {
                icqApiClient.sendMessageText(showIssueEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"));
            }
        } else if (currentUser != null) {
            icqApiClient.sendMessageText(showIssueEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.icqEventsListener.newIssueKeyMessage.error.issueNotFound"));
        }
        log.debug("ShowIssueEvent handling finished");
    }

    @Subscribe
    public void onShowDefaultMessageEvent(ShowDefaultMessageEvent showDefaultMessageEvent) throws IOException, UnirestException {
        log.debug("ShowDefaultMessageEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showDefaultMessageEvent.getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            icqApiClient.sendMessageText(showDefaultMessageEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.defaultMessage.text"), messageFormatter.getMenuButtons(locale));
        }
        log.debug("ShowDefaultMessageEvent handling finished");
    }

    @Subscribe
    public void onSearchIssuesClickEvent(SearchIssuesClickEvent searchIssuesClickEvent) throws IOException, UnirestException, SearchException {
        log.debug("ShowIssuesFilterResultsEvent handling started");
        JiraThreadLocalUtils.preCall();
        try {
            ApplicationUser currentUser = userData.getUserByMrimLogin(searchIssuesClickEvent.getUserId());
            if (currentUser != null) {
                Locale locale = localeManager.getLocaleFor(currentUser);
                SearchService.ParseResult parseResult = searchService.parseQuery(currentUser, searchIssuesClickEvent.getJqlClause());
                if (parseResult.isValid()) {
                    Query jqlQuery = parseResult.getQuery();
                    Query sanitizedJql = searchService.sanitiseSearchQuery(currentUser, jqlQuery);
                    PagerFilter<Issue> pagerFilter = new PagerFilter<>(0, LIST_PAGE_SIZE);
                    SearchResults<Issue> searchResults = searchService.search(currentUser, sanitizedJql, pagerFilter);

                    icqApiClient.answerCallbackQuery(searchIssuesClickEvent.getQueryId());
                    icqApiClient.sendMessageText(searchIssuesClickEvent.getChatId(),
                                                 messageFormatter.stringifyIssueList(searchResults.getResults(), 0, LIST_PAGE_SIZE),
                                                 messageFormatter.getListButtons(locale, false, searchResults.getTotal() > LIST_PAGE_SIZE));

                    chatsStateMap.put(searchIssuesClickEvent.getChatId(), ChatState.buildSearchResultsWatchingState(sanitizedJql, 0));
                } else {
                    icqApiClient.answerCallbackQuery(searchIssuesClickEvent.getQueryId());
                    icqApiClient.sendMessageText(searchIssuesClickEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.searchIssues.jqlParseError.text"));
                }

            }
            log.debug("ShowIssuesFilterResultsEvent handling finished");
        } finally {
            JiraThreadLocalUtils.postCall();
        }
    }

    @Subscribe
    public void onSearchIssuesClickEvent(SearchIssuesEvent searchIssuesClickEvent) throws IOException, UnirestException, SearchException {
        log.debug("ShowIssuesFilterResultsEvent handling started");
        JiraThreadLocalUtils.preCall();
        try {
            ApplicationUser currentUser = userData.getUserByMrimLogin(searchIssuesClickEvent.getUserId());
            if (currentUser != null) {
                Locale locale = localeManager.getLocaleFor(currentUser);
                SearchService.ParseResult parseResult = searchService.parseQuery(currentUser, searchIssuesClickEvent.getJqlClause());
                if (parseResult.isValid()) {
                    Query jqlQuery = parseResult.getQuery();
                    Query sanitizedJql = searchService.sanitiseSearchQuery(currentUser, jqlQuery);
                    PagerFilter<Issue> pagerFilter = new PagerFilter<>(0, LIST_PAGE_SIZE);
                    SearchResults<Issue> searchResults = searchService.search(currentUser, sanitizedJql, pagerFilter);
                    icqApiClient.sendMessageText(searchIssuesClickEvent.getChatId(),
                                                 messageFormatter.stringifyIssueList(searchResults.getResults(), 0, LIST_PAGE_SIZE),
                                                 messageFormatter.getListButtons(locale, false, searchResults.getTotal() > LIST_PAGE_SIZE));

                    chatsStateMap.put(searchIssuesClickEvent.getChatId(), ChatState.buildSearchResultsWatchingState(sanitizedJql, 0));
                } else {
                    icqApiClient.sendMessageText(searchIssuesClickEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.searchIssues.jqlParseError.text"));
                }
            }
            log.debug("ShowIssuesFilterResultsEvent handling finished");
        } finally {
            JiraThreadLocalUtils.postCall();
        }
    }


    @Subscribe
    public void onNextListPageClickEvent(NextPageClickEvent nextPageClickEvent) throws SearchException, UnirestException, IOException {
        log.debug("(NextPageClickEvent handling started");
        JiraThreadLocalUtils.preCall();
        try {
            ApplicationUser currentUser = userData.getUserByMrimLogin(nextPageClickEvent.getUserId());
            int nextPageStartIndex = (nextPageClickEvent.getCurrentPage() + 1) * LIST_PAGE_SIZE;
            if (currentUser != null) {
                Locale locale = localeManager.getLocaleFor(currentUser);
                PagerFilter<Issue> pagerFilter = new PagerFilter<>(nextPageStartIndex, LIST_PAGE_SIZE);
                SearchResults<Issue> searchResults = searchService.search(currentUser, nextPageClickEvent.getCurrentJqlQueryClause(), pagerFilter);
                icqApiClient.answerCallbackQuery(nextPageClickEvent.getQueryId());
                icqApiClient.editMessageText(nextPageClickEvent.getChatId(),
                                             nextPageClickEvent.getMsgId(),
                                             messageFormatter.stringifyIssueList(searchResults.getResults(), nextPageClickEvent.getCurrentPage() + 1, LIST_PAGE_SIZE),
                                             messageFormatter.getListButtons(locale, true, searchResults.getTotal() > LIST_PAGE_SIZE + nextPageStartIndex));
                chatsStateMap.put(nextPageClickEvent.getChatId(), ChatState.buildSearchResultsWatchingState(nextPageClickEvent.getCurrentJqlQueryClause(), nextPageClickEvent.getCurrentPage() + 1));
            }
            log.debug("(NextPageClickEvent handling finished");
        } finally {
            JiraThreadLocalUtils.postCall();
        }
    }

    @Subscribe
    public void onPrevListPageClickEvent(PrevPageClickEvent prevPageClickEvent) throws UnirestException, IOException, SearchException {
        log.debug("PrevPageClickEvent handling started");
        JiraThreadLocalUtils.preCall();
        try {
            ApplicationUser currentUser = userData.getUserByMrimLogin(prevPageClickEvent.getUserId());
            int prevPageStartIndex = (prevPageClickEvent.getCurrentPage() - 1) * LIST_PAGE_SIZE;
            if (currentUser != null) {
                Locale locale = localeManager.getLocaleFor(currentUser);
                PagerFilter<Issue> pagerFilter = new PagerFilter<>(prevPageStartIndex, LIST_PAGE_SIZE);
                SearchResults<Issue> searchResults = searchService.search(currentUser, prevPageClickEvent.getCurrentJqlQueryClause(), pagerFilter);
                icqApiClient.answerCallbackQuery(prevPageClickEvent.getQueryId());
                icqApiClient.editMessageText(prevPageClickEvent.getChatId(),
                                             prevPageClickEvent.getMsgId(),
                                             messageFormatter.stringifyIssueList(searchResults.getResults(), prevPageClickEvent.getCurrentPage() - 1, LIST_PAGE_SIZE),
                                             messageFormatter.getListButtons(locale, prevPageStartIndex >= LIST_PAGE_SIZE, true));
                chatsStateMap.put(prevPageClickEvent.getChatId(), ChatState.buildSearchResultsWatchingState(prevPageClickEvent.getCurrentJqlQueryClause(), prevPageClickEvent.getCurrentPage() - 1));
            }
            log.debug("PrevPageClickEvent handling finished");
        } finally {
            JiraThreadLocalUtils.postCall();
        }
    }

    @Subscribe
    public void onSearchByJqlClickEvent(SearchByJqlClickEvent searchByJqlClickEvent) throws UnirestException, IOException {
        log.debug("SearchByJqlClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(searchByJqlClickEvent.getUserId());
        String chatId = searchByJqlClickEvent.getChatId();
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            icqApiClient.answerCallbackQuery(searchByJqlClickEvent.getQueryId());
            icqApiClient.sendMessageText(searchByJqlClickEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.searchByJqlClauseButton.insertJqlClause.message"));
            chatsStateMap.put(chatId, ChatState.jqlClauseWaitingState());
        }
        log.debug("SearchByJqlClickEvent handling finished");
    }
}
