package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.CancelClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.CommentIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.IcqButtonClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.JiraNotifyEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.NewCommentMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.NewIssueKeyMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.SearchIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowHelpEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowMenuEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ViewIssueClickEvent;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class IcqEventsListener {
    private static final String THREAD_NAME_PREFIX = "icq-events-listener-thread-pool";
    private static final String CHAT_COMMAND_PREFIX = "/";

    private final ConcurrentHashMap<String, ChatState> chatsStateMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_PREFIX).build());
    private final Map<String, BotChatCommand> botChatCommandsMap;
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
        this.asyncEventBus = new AsyncEventBus(executorService, (exception, context) -> log.error(String.format("Event occurred in subscriber = %s", context.getSubscriber().toString()), exception));
        this.asyncEventBus.register(this);
        this.icqApiClient = icqApiClient;
        this.userData = userData;
        this.messageFormatter = messageFormatter;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.commentManager = commentManager;
        this.botChatCommandsMap =
                ImmutableMap.of(
                        "menu", (newMessageEvent) -> asyncEventBus.post(new ShowMenuEvent(newMessageEvent)),
                        "help", (newMessageEvent) -> {asyncEventBus.post(new ShowHelpEvent(newMessageEvent));}
                        );
    }

    public void publishIcqNewMessageEvent(NewMessageEvent event) { asyncEventBus.post(event); }

    public void publishJiraNotifyEvent(JiraNotifyEvent jiraNotifyEvent) {
        asyncEventBus.post(jiraNotifyEvent);
    }

    public void postIcqButtonClickEvent(IcqButtonClickEvent icqButtonClickEvent) { asyncEventBus.post(icqButtonClickEvent); }

    @Subscribe
    public void handleNewMessageEvent(NewMessageEvent newMessageEvent) throws Exception {
        String chatId = newMessageEvent.getChat().getChatId();
        if (chatsStateMap.containsKey(chatId)) {
            ChatState chatState = chatsStateMap.get(chatId);
            if (chatState.isWaitingForComment()) {
                asyncEventBus.post(new NewCommentMessageEvent(newMessageEvent));
            }
            if (chatState.isWaitingIssueKey()) {
                asyncEventBus.post(new NewIssueKeyMessageEvent(newMessageEvent));
            }
        } else {
            String currentChatCommand = newMessageEvent.getText();
            if (currentChatCommand.contains(CHAT_COMMAND_PREFIX)) {
                Optional.ofNullable(botChatCommandsMap.get(StringUtils.substringAfter(currentChatCommand, CHAT_COMMAND_PREFIX)))
                        .ifPresent(command -> command.execute(newMessageEvent));
            }
        }
    }

    @Subscribe
    public void handleJiraNotifyEvent(JiraNotifyEvent jiraNotifyEvent) throws Exception{
        icqApiClient.sendMessageText(jiraNotifyEvent.getChatId(), jiraNotifyEvent.getMessage(), jiraNotifyEvent.getButtons());
    }

    @Subscribe
    public void onViewIssueButtonClick(ViewIssueClickEvent viewIssueClickEvent) throws UnirestException, IOException {
        CallbackQueryEvent callbackQueryEvent = viewIssueClickEvent.getCallbackQueryEvent();
        String issueKey = StringUtils.substringAfter(callbackQueryEvent.getCallbackData(), "-");
        String chatId = callbackQueryEvent.getMessage().getChat().getChatId();
        String queryId = callbackQueryEvent.getQueryId();
        String mrimLogin = callbackQueryEvent.getFrom().getUserId();
        icqApiClient.answerCallbackQuery(queryId);
        Issue currentIssue = issueManager.getIssueByCurrentKey(issueKey);
        ApplicationUser currentUser = userData.getUserByMrimLogin(mrimLogin);
        if (currentUser != null && currentIssue != null) {
            if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, currentIssue, currentUser)) {
                icqApiClient.sendMessageText(chatId, messageFormatter.createIssueSummary(currentIssue, currentUser), messageFormatter.getIssueButtons(issueKey, currentUser));
                log.debug("ViewIssueCommand message sent...");
            } else {
                icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"), null);
                log.debug("ViewIssueCommand no permissions message sent...");
            }
        }
        log.debug("ViewIssueCommand execution finished...");
    }

    @Subscribe
    public void onCommentIssueButtonClick(CommentIssueClickEvent commentIssueClickEvent) throws UnirestException, IOException {
        log.debug("CreateCommentCommand execution started...");
        CallbackQueryEvent callbackQueryEvent = commentIssueClickEvent.getCallbackQueryEvent();
        String queryId = callbackQueryEvent.getQueryId();
        String chatId = callbackQueryEvent.getMessage().getChat().getChatId();
        String issueKey = StringUtils.substringAfter(callbackQueryEvent.getCallbackData(), "-");
        String mrimLogin = callbackQueryEvent.getFrom().getUserId();
        ApplicationUser commentedUser = userData.getUserByMrimLogin(mrimLogin);
        Locale locale = localeManager.getLocaleFor(commentedUser);
        String message = i18nResolver.getText(locale, "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.insertComment.message", issueKey);
        icqApiClient.answerCallbackQuery(queryId);
        icqApiClient.sendMessageText(chatId, message, messageFormatter.getCancelButton(commentedUser));
        chatsStateMap.put(chatId, ChatState.buildCommentWaitingState(issueKey));
        log.debug("JiraMessageQueueProcessor answerCommentButtonClick queue offer finished...");
    }

    @Subscribe
    public void handleNewCommentMessageEvent(NewCommentMessageEvent newCommentMessageEvent) throws IOException, UnirestException {
        NewMessageEvent newMessageEvent = newCommentMessageEvent.getNewMessageEvent();
        log.debug("CreateCommentCommand execution started...");
        String chatId = newMessageEvent.getChat().getChatId();
        String issueKey = (String)chatsStateMap.remove(chatId).getStateData();
        String mrimLogin = newMessageEvent.getFrom().getUserId();
        String commentMessage = newMessageEvent.getText();
        ApplicationUser commentedUser = userData.getUserByMrimLogin(mrimLogin);
        Issue commentedIssue = issueManager.getIssueByCurrentKey(issueKey);
        if (commentedUser != null && commentedIssue != null) {
            if (permissionManager.hasPermission(ProjectPermissions.ADD_COMMENTS, commentedIssue, commentedUser)) {
                commentManager.create(commentedIssue, commentedUser, commentMessage, true);
                log.debug("CreateCommentCommand comment created...");
                icqApiClient.sendMessageText(chatId, i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.commentCreated"), null);
                log.debug("CreateCommentCommand new comment created message sent...");
            } else {
                log.debug("CreateCommentCommand permissions violation occurred...");
                icqApiClient.sendMessageText(chatId,i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.noPermissions"), null);
                log.debug("CreateCommentCommand not enough permissions message sent...");
            }
        }
        log.debug("CreateCommentCommand execution finished...");
    }

    @Subscribe
    public void onCancelButtonClick(CancelClickEvent cancelClickEvent) throws UnirestException {
        log.debug("CancelCommand execution started...");
        String queryId = cancelClickEvent.getCallbackQueryEvent().getQueryId();
        String userId = cancelClickEvent.getCallbackQueryEvent().getFrom().getUserId();
        String message = i18nResolver.getRawText(localeManager.getLocaleFor(userData.getUserByMrimLogin(userId)), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.cancelComment.message");
        icqApiClient.answerCallbackQuery(queryId, message, false, null);
        chatsStateMap.remove(cancelClickEvent.getCallbackQueryEvent().getMessage().getChat().getChatId());
        log.debug("CancelCommand execution finished...");
    }

    @Subscribe
    public void onShowMenuEvent(ShowMenuEvent showMenuEvent) throws IOException, UnirestException {
        log.debug("ShowDefaultMenuEvent handling started...");
        NewMessageEvent newMessageEvent = showMenuEvent.getNewMessageEvent();
        String chatId = newMessageEvent.getChat().getChatId();
        ApplicationUser currentUser = userData.getUserByMrimLogin(newMessageEvent.getFrom().getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.mainMenu.text"), messageFormatter.getMenuButtons(locale));
        }
        log.debug("JiraMessageQueueProcessor showDefaultMenu finished...");
    }

    @Subscribe
    public void onShowHelpEvent(ShowHelpEvent showHelpEvent) throws IOException, UnirestException {
        log.debug("ShowHelpEvent handling started");
        NewMessageEvent newMessageEvent = showHelpEvent.getNewMessageEvent();
        String chatId = newMessageEvent.getChat().getChatId();
        ApplicationUser currentUser = userData.getUserByMrimLogin(newMessageEvent.getFrom().getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.helpMessage.text"), null);
        }
        log.debug("ShowHelpEvent handling finished");
    }

    @Subscribe
    public void onSearchIssueButtonClick(SearchIssueClickEvent searchIssueClickEvent) throws IOException, UnirestException {
        log.debug("OnSearchIssueButtonClick event handling started");
        CallbackQueryEvent callbackQueryEvent = searchIssueClickEvent.getCallbackQueryEvent();
        ApplicationUser currentUser = userData.getUserByMrimLogin(callbackQueryEvent.getFrom().getUserId());
        String chatId = callbackQueryEvent.getMessage().getChat().getChatId();
        if (currentUser != null) {
            String message = i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.searchButton.insertIssueKey.message");
            icqApiClient.answerCallbackQuery(callbackQueryEvent.getQueryId());
            icqApiClient.sendMessageText(chatId, message, messageFormatter.getCancelButton(currentUser));
        }
        chatsStateMap.put(chatId, ChatState.buildIssueKeyWaitingState());
        log.debug("OnSearchIssueButtonClick event handling finished");
    }

    @Subscribe
    public void handleNewIssueKeyMessageEvent(NewIssueKeyMessageEvent newIssueKeyMessageEvent) throws IOException, UnirestException {
        log.debug("NewIssueKeyMessageEvent handling started");
        NewMessageEvent newMessageEvent = newIssueKeyMessageEvent.getNewMessageEvent();
        String chatId = newMessageEvent.getChat().getChatId();
        chatsStateMap.remove(chatId);
        ApplicationUser currentUser = userData.getUserByMrimLogin(newMessageEvent.getFrom().getUserId());
        String newMessageText = newMessageEvent.getText().trim();
        Issue currentIssue = issueManager.getIssueByCurrentKey(newMessageText);
        if (currentUser != null && currentIssue != null) {
            if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, currentIssue, currentUser)) {
                icqApiClient.sendMessageText(chatId, messageFormatter.createIssueSummary(currentIssue, currentUser), messageFormatter.getIssueButtons(currentIssue.getKey(), currentUser));
                log.debug("ViewIssueCommand message sent...");
            } else {
                icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"), null);
                log.debug("ViewIssueCommand no permissions message sent...");
            }
        } else if (currentIssue == null && currentUser != null) {
            icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.icqEventsListener.newIssueKeyMessage.error.issueNotFound"), null);
        }
        log.debug("NewIssueKeyMessageEvent handling finished");
    }
}
