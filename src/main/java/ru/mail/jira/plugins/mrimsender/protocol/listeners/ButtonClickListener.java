package ru.mail.jira.plugins.mrimsender.protocol.listeners;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.query.Query;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.Subscribe;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.protocol.ChatState;
import ru.mail.jira.plugins.mrimsender.protocol.ChatStateMapping;
import ru.mail.jira.plugins.mrimsender.protocol.MessageFormatter;
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
import java.util.stream.Collectors;

import static ru.mail.jira.plugins.mrimsender.protocol.MessageFormatter.LIST_PAGE_SIZE;

@Slf4j
public class ButtonClickListener {
    private final ConcurrentHashMap<String, ChatState> chatsStateMap;
    private final IcqApiClient icqApiClient;
    private final UserData userData;
    private final MessageFormatter messageFormatter;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final IssueManager issueManager;
    private final PermissionManager permissionManager;
    private final SearchService searchService;
    private final ProjectManager projectManager;
    private final IssueTypeSchemeManager issueTypeSchemeManager;

    public ButtonClickListener(ChatStateMapping chatStateMapping,
                               IcqApiClient icqApiClient,
                               UserData userData,
                               MessageFormatter messageFormatter,
                               I18nResolver i18nResolver,
                               LocaleManager localeManager,
                               IssueManager issueManager,
                               PermissionManager permissionManager,
                               SearchService searchService,
                               ProjectManager projectManager,
                               IssueTypeSchemeManager issueTypeSchemeManager) {
        this.chatsStateMap = chatStateMapping.getChatsStateMap();
        this.icqApiClient = icqApiClient;
        this.userData = userData;
        this.messageFormatter = messageFormatter;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.searchService = searchService;
        this.projectManager = projectManager;
        this.issueTypeSchemeManager = issueTypeSchemeManager;
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
    public void onCancelButtonClick(CancelClickEvent cancelClickEvent) throws UnirestException {
        log.debug("CancelCommand execution started...");
        String message = i18nResolver.getRawText(localeManager.getLocaleFor(userData.getUserByMrimLogin(cancelClickEvent.getUserId())), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.cancelComment.message");
        icqApiClient.answerCallbackQuery(cancelClickEvent.getQueryId(), message, false, null);
        chatsStateMap.remove(cancelClickEvent.getChatId());
        log.debug("CancelCommand execution finished...");
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
                                                 messageFormatter.stringifyIssueList(locale, searchResults.getResults(), 0, searchResults.getTotal()),
                                                 messageFormatter.getIssueListButtons(locale, false, searchResults.getTotal() > LIST_PAGE_SIZE));

                    chatsStateMap.put(searchIssuesClickEvent.getChatId(), ChatState.buildIssueSearchResultsWatchingState(sanitizedJql, 0));
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
    public void onNextIssuesPageClickEvent(NextIssuesPageClickEvent nextIssuesPageClickEvent) throws SearchException, UnirestException, IOException {
        log.debug("(NextPageClickEvent handling started");
        JiraThreadLocalUtils.preCall();
        try {
            ApplicationUser currentUser = userData.getUserByMrimLogin(nextIssuesPageClickEvent.getUserId());
            int nextPageStartIndex = (nextIssuesPageClickEvent.getCurrentPage() + 1) * LIST_PAGE_SIZE;
            if (currentUser != null) {
                Locale locale = localeManager.getLocaleFor(currentUser);
                PagerFilter<Issue> pagerFilter = new PagerFilter<>(nextPageStartIndex, LIST_PAGE_SIZE);
                SearchResults<Issue> searchResults = searchService.search(currentUser, nextIssuesPageClickEvent.getCurrentJqlQueryClause(), pagerFilter);
                icqApiClient.answerCallbackQuery(nextIssuesPageClickEvent.getQueryId());
                icqApiClient.editMessageText(nextIssuesPageClickEvent.getChatId(),
                                             nextIssuesPageClickEvent.getMsgId(),
                                             messageFormatter.stringifyIssueList(locale, searchResults.getResults(), nextIssuesPageClickEvent.getCurrentPage() + 1, searchResults.getTotal()),
                                             messageFormatter.getIssueListButtons(locale, true, searchResults.getTotal() > LIST_PAGE_SIZE + nextPageStartIndex));
                chatsStateMap.put(nextIssuesPageClickEvent.getChatId(), ChatState.buildIssueSearchResultsWatchingState(nextIssuesPageClickEvent.getCurrentJqlQueryClause(), nextIssuesPageClickEvent.getCurrentPage() + 1));
            }
            log.debug("(NextPageClickEvent handling finished");
        } finally {
            JiraThreadLocalUtils.postCall();
        }
    }

    @Subscribe
    public void onPrevIssuesPageClickEvent(PrevIssuesPageClickEvent prevIssuesPageClickEvent) throws UnirestException, IOException, SearchException {
        log.debug("NextProjectsPageClickEvent handling started");
        JiraThreadLocalUtils.preCall();
        try {
            ApplicationUser currentUser = userData.getUserByMrimLogin(prevIssuesPageClickEvent.getUserId());
            int prevPageStartIndex = (prevIssuesPageClickEvent.getCurrentPage() - 1) * LIST_PAGE_SIZE;
            if (currentUser != null) {
                Locale locale = localeManager.getLocaleFor(currentUser);
                PagerFilter<Issue> pagerFilter = new PagerFilter<>(prevPageStartIndex, LIST_PAGE_SIZE);
                SearchResults<Issue> searchResults = searchService.search(currentUser, prevIssuesPageClickEvent.getCurrentJqlQueryClause(), pagerFilter);
                icqApiClient.answerCallbackQuery(prevIssuesPageClickEvent.getQueryId());
                icqApiClient.editMessageText(prevIssuesPageClickEvent.getChatId(),
                                             prevIssuesPageClickEvent.getMsgId(),
                                             messageFormatter.stringifyIssueList(locale, searchResults.getResults(), prevIssuesPageClickEvent.getCurrentPage() - 1, searchResults.getTotal()),
                                             messageFormatter.getIssueListButtons(locale, prevPageStartIndex >= LIST_PAGE_SIZE, true));
                chatsStateMap.put(prevIssuesPageClickEvent.getChatId(), ChatState.buildIssueSearchResultsWatchingState(prevIssuesPageClickEvent.getCurrentJqlQueryClause(), prevIssuesPageClickEvent.getCurrentPage() - 1));
            }
            log.debug("NextProjectsPageClickEvent handling finished");
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
            icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.searchByJqlClauseButton.insertJqlClause.message"));
            chatsStateMap.put(chatId, ChatState.jqlClauseWaitingState());
        }
        log.debug("SearchByJqlClickEvent handling finished");
    }

    @Subscribe
    public void onCreateIssueClickEvent(CreateIssueClickEvent createIssueClickEvent) throws UnirestException, IOException {
        log.debug("CreateIssueClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(createIssueClickEvent.getUserId());
        String chatId = createIssueClickEvent.getChatId();
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            icqApiClient.answerCallbackQuery(createIssueClickEvent.getQueryId());
            List<Project> projectList = projectManager.getProjects();
            List<Project> firstPageProjectsInterval = projectList.stream().limit(LIST_PAGE_SIZE).collect(Collectors.toList());
            icqApiClient.sendMessageText(chatId,
                                         messageFormatter.createSelectProjectMessage(locale, firstPageProjectsInterval, 0, projectList.size()),
                                         messageFormatter.getSelectProjectMessageButtons(locale, false, projectList.size() > LIST_PAGE_SIZE));
            chatsStateMap.put(chatId, ChatState.buildProjectSelectWaitingState(0));
        }
        log.debug("CreateIssueClickEvent handling finished");
    }

    @Subscribe
    public void onNextProjectPageClickEvent(NextProjectsPageClickEvent nextProjectsPageClickEvent) throws UnirestException, IOException {
        log.debug("NextProjectsPageClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(nextProjectsPageClickEvent.getUserId());
        if (currentUser != null) {
            int nextPageNumber = nextProjectsPageClickEvent.getCurrentPage() + 1;
            int nextPageStartIndex = nextPageNumber * LIST_PAGE_SIZE;
            Locale locale = localeManager.getLocaleFor(currentUser);
            String chatId = nextProjectsPageClickEvent.getChatId();

            List<Project> projectsList = projectManager.getProjects();
            List<Project> nextProjectsInterval = projectsList.stream()
                                                             .skip(nextPageStartIndex)
                                                             .limit(LIST_PAGE_SIZE)
                                                             .collect(Collectors.toList());

            icqApiClient.answerCallbackQuery(nextProjectsPageClickEvent.getQueryId());
            icqApiClient.editMessageText(chatId,
                                         nextProjectsPageClickEvent.getMsgId(),
                                         messageFormatter.createSelectProjectMessage(locale, nextProjectsInterval, nextPageNumber, projectsList.size()),
                                         messageFormatter.getSelectProjectMessageButtons(locale, true, projectsList.size() > LIST_PAGE_SIZE + nextPageStartIndex));
            chatsStateMap.put(chatId, ChatState.buildProjectSelectWaitingState(nextPageNumber));
        }
        log.debug("NextProjectsPageClickEvent handling finished");
    }

    @Subscribe
    public void onPrevProjectPageClickEvent(PrevProjectsPageClickEvent prevProjectsPageClickEvent) throws UnirestException, IOException {
        log.debug("PrevProjectsPageClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(prevProjectsPageClickEvent.getUserId());
        if (currentUser != null) {
            int prevPageNumber = prevProjectsPageClickEvent.getCurrentPage() - 1;
            int prevPageStartIndex = prevPageNumber * LIST_PAGE_SIZE;
            Locale locale = localeManager.getLocaleFor(currentUser);
            String chatId = prevProjectsPageClickEvent.getChatId();

            List<Project> projectsList = projectManager.getProjects();
            List<Project> prevProjectsInterval = projectsList.stream()
                                                             .skip(prevPageStartIndex)
                                                             .limit(LIST_PAGE_SIZE)
                                                             .collect(Collectors.toList());
            icqApiClient.answerCallbackQuery(prevProjectsPageClickEvent.getQueryId());
            icqApiClient.editMessageText(chatId,
                                         prevProjectsPageClickEvent.getMsgId(),
                                         messageFormatter.createSelectProjectMessage(locale, prevProjectsInterval, prevPageNumber,  projectsList.size()),
                                         messageFormatter.getSelectProjectMessageButtons(locale, prevPageStartIndex >= LIST_PAGE_SIZE, true));
            chatsStateMap.put(chatId, ChatState.buildProjectSelectWaitingState(prevPageNumber));
        }
        log.debug("PrevProjectsPageClickEvent handling finished");
    }

    @Subscribe
    public void onNextIssueTypesPageClickEvent(NextIssueTypesPageClickEvent nextIssueTypesPageClickEvent) throws UnirestException, IOException {
        log.debug("NextIssueTypesPageClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(nextIssueTypesPageClickEvent.getUserId());
        if (currentUser != null) {
            int nextPageNumber = nextIssueTypesPageClickEvent.getCurrentPage() + 1;
            int nextPageStartIndex = nextPageNumber  * LIST_PAGE_SIZE;
            Locale locale = localeManager.getLocaleFor(currentUser);
            String chatId = nextIssueTypesPageClickEvent.getChatId();
            String selectedProjectKey = nextIssueTypesPageClickEvent.getSelectedProjectKey();

            Collection<IssueType> issueTypeList = issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(projectManager.getProjectByCurrentKey(selectedProjectKey));
            List<IssueType> issueTypeListInterval = issueTypeList.stream()
                                                                       .skip(nextPageStartIndex)
                                                                       .limit(LIST_PAGE_SIZE)
                                                                       .collect(Collectors.toList());
            icqApiClient.answerCallbackQuery(nextIssueTypesPageClickEvent.getQueryId());
            icqApiClient.editMessageText(chatId,
                                         nextIssueTypesPageClickEvent.getMsgId(),
                                         messageFormatter.createSelectIssueTypeMessage(locale, issueTypeListInterval, nextPageNumber,issueTypeList.size()),
                                         messageFormatter.getSelectIssueTypeMessageButtons(locale, true, issueTypeList.size() > LIST_PAGE_SIZE + nextPageStartIndex));
            chatsStateMap.put(chatId, ChatState.buildIssueTypeSelectWaitingState(selectedProjectKey, nextPageNumber));
        }
        log.debug("NextIssueTypesPageClickEvent handling finished");
    }

    @Subscribe
    public void onPrevIssueTypesPageClickEvent(PrevIssueTypesPageClickEvent prevIssueTypesPageClickEvent) throws UnirestException, IOException {
        log.debug("PrevIssueTypesPageClickEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(prevIssueTypesPageClickEvent.getUserId());
        if (currentUser != null) {
            int prevPageNumber = prevIssueTypesPageClickEvent.getCurrentPage() - 1;
            int prevPageStartIndex = prevPageNumber  * LIST_PAGE_SIZE;
            Locale locale = localeManager.getLocaleFor(currentUser);
            String chatId = prevIssueTypesPageClickEvent.getChatId();
            String selectedProjectKey = prevIssueTypesPageClickEvent.getSelectedProjectKey();

            Collection<IssueType> issueTypeList = issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(projectManager.getProjectByCurrentKey(selectedProjectKey));
            List<IssueType> issueTypeListInterval = issueTypeList.stream()
                                                                 .skip(prevPageStartIndex)
                                                                 .limit(LIST_PAGE_SIZE)
                                                                 .collect(Collectors.toList());
            icqApiClient.answerCallbackQuery(prevIssueTypesPageClickEvent.getQueryId());
            icqApiClient.editMessageText(chatId,
                                         prevIssueTypesPageClickEvent.getMsgId(),
                                         messageFormatter.createSelectIssueTypeMessage(locale, issueTypeListInterval, prevPageNumber, issueTypeList.size()),
                                         messageFormatter.getSelectIssueTypeMessageButtons(locale, prevPageStartIndex >= LIST_PAGE_SIZE, true));
            chatsStateMap.put(chatId, ChatState.buildIssueTypeSelectWaitingState(selectedProjectKey, prevPageNumber));
        }
        log.debug("PrevIssueTypesPageClickEvent handling finished");
    }
}
