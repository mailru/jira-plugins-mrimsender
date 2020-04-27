package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.protocol.events.CancelClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.CommentIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.Event;
import ru.mail.jira.plugins.mrimsender.protocol.events.IssueKeyMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.JiraNotifyEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.NewCommentMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.SearchIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowHelpEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowIssueEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowMenuEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ViewIssueClickEvent;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class IcqEventsListener {
    private static final String THREAD_NAME_PREFIX = "icq-events-listener-thread-pool";
    private static final String CHAT_COMMAND_PREFIX = "/";

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

    public IcqEventsListener(IcqApiClient icqApiClient,
                             UserData userData,
                             MessageFormatter messageFormatter,
                             LocaleManager localeManager,
                             I18nResolver i18nResolver,
                             IssueManager issueManager,
                             PermissionManager permissionManager,
                             CommentManager commentManager) {
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
    }

    public void publishEvent(Event event) {
        asyncEventBus.post(event);
    }

    @Subscribe
    public void handleNewMessageEvent(ChatMessageEvent chatMessageEvent) throws Exception {
        String chatId = chatMessageEvent.getChatId();
        if (chatsStateMap.containsKey(chatId)) {
            ChatState chatState = chatsStateMap.remove(chatId);
            if (chatState.isWaitingForComment()) {
                asyncEventBus.post(new NewCommentMessageEvent(chatMessageEvent, chatState.getIssueKey()));
            }
            if (chatState.isWaitingIssueKey()) {
                asyncEventBus.post(new IssueKeyMessageEvent(chatMessageEvent));
            }
        } else {
            String currentChatCommand = chatMessageEvent.getMessage();
            if (currentChatCommand != null && currentChatCommand.startsWith(CHAT_COMMAND_PREFIX)) {
                String command = StringUtils.substringAfter(currentChatCommand, CHAT_COMMAND_PREFIX);
                if (command.startsWith("help")) {
                    asyncEventBus.post(new ShowHelpEvent(chatMessageEvent));
                }
                if (command.startsWith("menu")) {
                    asyncEventBus.post(new ShowMenuEvent(chatMessageEvent));
                }
                if (command.startsWith("issue")) {
                    asyncEventBus.post(new ShowIssueEvent(chatMessageEvent));
                }
            }
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
                icqApiClient.sendMessageText(viewIssueClickEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"), null);
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
                    icqApiClient.sendMessageText(newCommentMessageEvent.getChatId(), i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.commentCreated"), null);
                    log.debug("CreateCommentCommand new comment created message sent...");
                } else {
                    log.debug("CreateCommentCommand permissions violation occurred...");
                    icqApiClient.sendMessageText(newCommentMessageEvent.getChatId(), i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.noPermissions"), null);
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
            icqApiClient.sendMessageText(showHelpEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.helpMessage.text"), null);
        }
        log.debug("ShowHelpEvent handling finished");
    }

    @Subscribe
    public void onSearchIssueButtonClick(SearchIssueClickEvent searchIssueClickEvent) throws IOException, UnirestException {
        log.debug("OnSearchIssueButtonClick event handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(searchIssueClickEvent.getUserId());
        if (currentUser != null) {
            String message = i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.searchButton.insertIssueKey.message");
            icqApiClient.answerCallbackQuery(searchIssueClickEvent.getQueryId());
            icqApiClient.sendMessageText(searchIssueClickEvent.getChatId(), message, messageFormatter.getCancelButton(currentUser));
            chatsStateMap.put(searchIssueClickEvent.getChatId(), ChatState.issueKeyWaitingState());
        }
        log.debug("OnSearchIssueButtonClick event handling finished");
    }

    @Subscribe
    public void handleNewIssueKeyMessageEvent(IssueKeyMessageEvent issueKeyMessageEvent) throws IOException, UnirestException {
        log.debug("NewIssueKeyMessageEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(issueKeyMessageEvent.getUserId());
        Issue currentIssue = issueManager.getIssueByCurrentKey(issueKeyMessageEvent.getMessage());
        if (currentUser != null && currentIssue != null) {
            if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, currentIssue, currentUser)) {
                icqApiClient.sendMessageText(issueKeyMessageEvent.getChatId(), messageFormatter.createIssueSummary(currentIssue, currentUser), messageFormatter.getIssueButtons(currentIssue.getKey(), currentUser));
                log.debug("ViewIssueCommand message sent...");
            } else {
                icqApiClient.sendMessageText(issueKeyMessageEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"), null);
                log.debug("ViewIssueCommand no permissions message sent...");
            }
        } else if (currentUser != null) {
            icqApiClient.sendMessageText(issueKeyMessageEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.icqEventsListener.newIssueKeyMessage.error.issueNotFound"), null);
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
                icqApiClient.sendMessageText(showIssueEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"), null);
            }
        } else if (currentUser != null) {
            icqApiClient.sendMessageText(showIssueEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.icqEventsListener.newIssueKeyMessage.error.issueNotFound"), null);        }
        log.debug("ShowIssueEvent handling finished");
    }
}
