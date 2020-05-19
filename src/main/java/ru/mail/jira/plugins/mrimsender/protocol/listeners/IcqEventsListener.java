package ru.mail.jira.plugins.mrimsender.protocol.listeners;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
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
import ru.mail.jira.plugins.mrimsender.protocol.ChatState;
import ru.mail.jira.plugins.mrimsender.protocol.ChatStateMapping;
import ru.mail.jira.plugins.mrimsender.protocol.IssueCreationDto;
import ru.mail.jira.plugins.mrimsender.protocol.MessageFormatter;
import ru.mail.jira.plugins.mrimsender.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.Event;
import ru.mail.jira.plugins.mrimsender.protocol.events.IssueKeyMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.JiraNotifyEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.NewCommentMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.SearchIssuesEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.SelectedIssueTypeMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.SelectedProjectMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowDefaultMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowHelpEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowIssueEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowMenuEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.CancelClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.CommentIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.CreateIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.NextIssueTypesPageClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.NextIssuesPageClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.NextProjectsPageClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.PrevIssueTypesPageClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.PrevIssuesPageClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.PrevProjectsPageClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.SearchByJqlClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.SearchIssuesClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.ShowIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.ViewIssueClickEvent;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static ru.mail.jira.plugins.mrimsender.protocol.MessageFormatter.LIST_PAGE_SIZE;

@Slf4j
public class IcqEventsListener {
    private static final String THREAD_NAME_PREFIX = "icq-events-listener-thread-pool";
    private static final String CHAT_COMMAND_PREFIX = "/";

    private final ConcurrentHashMap<String, ChatState> chatsStateMap;
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
    private final ProjectManager projectManager;
    private final IssueTypeSchemeManager issueTypeSchemeManager;

    public IcqEventsListener(ChatStateMapping chatStateMapping,
                             IcqApiClient icqApiClient,
                             UserData userData,
                             MessageFormatter messageFormatter,
                             LocaleManager localeManager,
                             I18nResolver i18nResolver,
                             IssueManager issueManager,
                             PermissionManager permissionManager,
                             CommentManager commentManager,
                             SearchService searchService,
                             ProjectManager projectManager,
                             ChatCommandListener chatCommandListener,
                             ButtonClickListener buttonClickListener,
                             IssueTypeSchemeManager issueTypeSchemeManager) {
        this.chatsStateMap = chatStateMapping.getChatsStateMap();
        this.asyncEventBus = new AsyncEventBus(executorService, (exception, context) -> log.error(String.format("Exception occurred in subscriber = %s", context.getSubscriber().toString()), exception));
        this.asyncEventBus.register(this);
        this.asyncEventBus.register(chatCommandListener);
        this.asyncEventBus.register(buttonClickListener);
        this.icqApiClient = icqApiClient;
        this.userData = userData;
        this.messageFormatter = messageFormatter;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.commentManager = commentManager;
        this.searchService = searchService;
        this.projectManager = projectManager;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
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
            if (chatState.isWaitingForProjectSelect()) {
                asyncEventBus.post(new SelectedProjectMessageEvent(chatMessageEvent));
                return;
            }
            if (chatState.isWaitingForIssueTypeSelect()) {
                asyncEventBus.post(new SelectedIssueTypeMessageEvent(chatMessageEvent, chatState.getIssueCreationDto()));
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
    public void handleButtonClickEvent(ButtonClickEvent buttonClickEvent) throws UnirestException, IOException {
        String buttonPrefix = StringUtils.substringBefore(buttonClickEvent.getCallbackData(), "-");
        String chatId = buttonClickEvent.getChatId();
        boolean isGroupChatEvent = buttonClickEvent.getChatType() == ChatType.GROUP;

        // if chat is in some state then use our state processing logic
        if (!isGroupChatEvent && chatsStateMap.containsKey(chatId)) {
            ChatState chatState = chatsStateMap.remove(chatId);
            if (chatState.isIssueSearchResultsShowing()) {
                if (buttonPrefix.equals("nextIssueListPage")) {
                    asyncEventBus.post(new NextIssuesPageClickEvent(buttonClickEvent, chatState.getCurrentSelectListPage(), chatState.getCurrentSearchJqlClause()));
                    return;
                }
                if (buttonPrefix.equals("prevIssueListPage")) {
                    asyncEventBus.post(new PrevIssuesPageClickEvent(buttonClickEvent, chatState.getCurrentSelectListPage(), chatState.getCurrentSearchJqlClause()));
                    return;
                }
            }
            if (chatState.isWaitingForProjectSelect()) {
                if (buttonPrefix.equals("nextProjectListPage")) {
                    asyncEventBus.post(new NextProjectsPageClickEvent(buttonClickEvent, chatState.getCurrentSelectListPage()));
                    return;
                }
                if (buttonPrefix.equals("prevProjectListPage")) {
                    asyncEventBus.post(new PrevProjectsPageClickEvent(buttonClickEvent, chatState.getCurrentSelectListPage()));
                    return;
                }
            }
            if (chatState.isWaitingForIssueTypeSelect()) {
                if (buttonPrefix.equals("nextIssueTypeListPage")) {
                    asyncEventBus.post(new NextIssueTypesPageClickEvent(buttonClickEvent, chatState.getCurrentSelectListPage(), chatState.getSelectedProjectKey()));
                    return;
                }
                if (buttonPrefix.equals("prevIssueTypeListPage")) {
                    asyncEventBus.post(new PrevIssueTypesPageClickEvent(buttonClickEvent, chatState.getCurrentSelectListPage(), chatState.getSelectedProjectKey()));
                    return;
                }
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
            case "createIssue":
                asyncEventBus.post(new CreateIssueClickEvent(buttonClickEvent));
            default:
                // fix infinite spinners situations for not recognized button clicks
                // for example next or prev button click when chat state was cleared
                icqApiClient.answerCallbackQuery(buttonClickEvent.getQueryId());
                break;
        }
    }

    @Subscribe
    public void handleJiraNotifyEvent(JiraNotifyEvent jiraNotifyEvent) throws Exception {
        icqApiClient.sendMessageText(jiraNotifyEvent.getChatId(), jiraNotifyEvent.getMessage(), jiraNotifyEvent.getButtons());
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
    public void onSearchIssuesEvent(SearchIssuesEvent searchIssuesEvent) throws IOException, UnirestException, SearchException {
        log.debug("ShowIssuesFilterResultsEvent handling started");
        JiraThreadLocalUtils.preCall();
        try {
            ApplicationUser currentUser = userData.getUserByMrimLogin(searchIssuesEvent.getUserId());
            if (currentUser != null) {
                Locale locale = localeManager.getLocaleFor(currentUser);
                SearchService.ParseResult parseResult = searchService.parseQuery(currentUser, searchIssuesEvent.getJqlClause());
                if (parseResult.isValid()) {
                    Query jqlQuery = parseResult.getQuery();
                    Query sanitizedJql = searchService.sanitiseSearchQuery(currentUser, jqlQuery);
                    MessageSet messageSet = searchService.validateQuery(currentUser, sanitizedJql);
                    if (!messageSet.hasAnyErrors()) {
                        PagerFilter<Issue> pagerFilter = new PagerFilter<>(0, LIST_PAGE_SIZE);
                        SearchResults<Issue> searchResults = searchService.search(currentUser, sanitizedJql, pagerFilter);
                        icqApiClient.sendMessageText(searchIssuesEvent.getChatId(),
                                                     messageFormatter.stringifyIssueList(locale, searchResults.getResults(), 0, searchResults.getTotal()),
                                                     messageFormatter.getIssueListButtons(locale, false, searchResults.getTotal() > LIST_PAGE_SIZE));

                        chatsStateMap.put(searchIssuesEvent.getChatId(), ChatState.buildIssueSearchResultsWatchingState(sanitizedJql, 0));
                    } else {
                        icqApiClient.sendMessageText(searchIssuesEvent.getChatId(),
                                                     String.join("\n", i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.searchIssues.jqlParseError.text"), messageFormatter.stringifyJqlClauseErrorsMap(messageSet, locale)));
                    }
                } else {
                    icqApiClient.sendMessageText(searchIssuesEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.searchIssues.jqlParseError.text"));
                }
            }
            log.debug("ShowIssuesFilterResultsEvent handling finished");
        } finally {
            JiraThreadLocalUtils.postCall();
        }
    }

    @Subscribe
    public void onSelectedProjectMessageEvent(SelectedProjectMessageEvent selectedProjectMessageEvent) throws IOException, UnirestException {
        ApplicationUser currentUser = userData.getUserByMrimLogin(selectedProjectMessageEvent.getUserId());
        if (currentUser != null) {
            String chatId = selectedProjectMessageEvent.getChatId();
            Locale locale = localeManager.getLocaleFor(currentUser);
            String selectedProjectKey = selectedProjectMessageEvent.getSelectedProjectKey();
            Project selectedProject = projectManager.getProjectByCurrentKey(selectedProjectKey);
            if (selectedProject != null) {
                // Project selected, sending user select IssueType message
                Collection<IssueType> projectIssueTypes = issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(selectedProject);
                icqApiClient.sendMessageText(chatId,
                                             messageFormatter.createSelectIssueTypeMessage(locale,
                                                                                           projectIssueTypes.stream().limit(LIST_PAGE_SIZE).collect(Collectors.toList()),
                                                                                           0,
                                                                                           projectIssueTypes.size()),
                                             messageFormatter.getSelectIssueTypeMessageButtons(locale, false, projectIssueTypes.size() > LIST_PAGE_SIZE));
                chatsStateMap.put(chatId, ChatState.buildIssueTypeSelectWaitingState( selectedProjectKey, 0));
            } else {
                // TODO project key is not valid ... maybe suggest enter project key again ???
            }
        }
    }

    @Subscribe void onSelectedIssueTypeMessageEvent(SelectedIssueTypeMessageEvent selectedIssueTypeMessageEvent) throws IOException, UnirestException {
        ApplicationUser currentUser = userData.getUserByMrimLogin(selectedIssueTypeMessageEvent.getUserId());
        if (currentUser != null) {
            IssueCreationDto currentIssueCreationDto = selectedIssueTypeMessageEvent.getIssueCreationDto();
            Project selectedProject = projectManager.getProjectByCurrentKey(currentIssueCreationDto.getProjectKey());
            String selectedIssueTypePosition = selectedIssueTypeMessageEvent.getSelectedIssueTypePosition();
            String chatId = selectedIssueTypeMessageEvent.getChatId();
            Locale locale = localeManager.getLocaleFor(currentUser);


            if (selectedProject != null && StringUtils.isNumeric(selectedIssueTypePosition)) {
                List<IssueType> selectedIssueTypeBoxed = issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(selectedProject)
                                                                   .stream()
                                                                   .sorted()
                                                                   .skip(Integer.parseInt(selectedIssueTypePosition) - 1)
                                                                   .limit(1)
                                                                   .collect(Collectors.toList());
                if (selectedIssueTypeBoxed.size() > 0) {
                    IssueType selectedIssueType = selectedIssueTypeBoxed.get(0);
                    currentIssueCreationDto.setIssueTypeId(selectedIssueType.getId());

                } else {
                    // TODO issueType number is not valid... maybe
                }
            } else {
                // TODO issueType number is not valid ... maybe suggest enter issue type again ???
            }
        }
    }

}
