package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.Chat;
import ru.mail.jira.plugins.mrimsender.icq.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.mrimsender.icq.dto.User;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class IcqBotChatHandlers {
    private final ConcurrentHashMap<String, String> chatsStateMap = new ConcurrentHashMap<>();
    private final IcqApiClient icqApiClient;
    private final CommentManager commentManager;
    private final UserData userData;
    private final IssueManager issueManager;
    private final MessageFormatter messageFormatter;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final PermissionManager permissionManager;

    public IcqBotChatHandlers(IcqApiClient icqApiClient,
                                     CommentManager commentManager,
                                     UserData userData, IssueManager issueManager,
                                     MessageFormatter messageFormatter,
                                     LocaleManager localeManager,
                                     I18nResolver i18nResolver,
                                     PermissionManager permissionManager) {
        this.commentManager = commentManager;
        this.userData = userData;
        this.issueManager = issueManager;
        this.icqApiClient = icqApiClient;
        this.messageFormatter = messageFormatter;
        this.localeManager = localeManager;
        this.i18nResolver = i18nResolver;
        this.permissionManager = permissionManager;
    }

    public void sendJiraNotificationMessage(String chatId, String message, List<List<InlineKeyboardMarkupButton>> buttons) throws IOException, UnirestException {
        log.debug("JiraMessageQueueProcessor sendMessage started...");
        icqApiClient.sendMessageText(chatId, message, buttons);
        log.debug("JiraMessageQueueProcessor sendMessage finished...");
    }

    public void handleNewJiraCommentCreated(String chatId, String mrimLogin, String commentMessage) throws IOException, UnirestException {
        log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated started...");
        String issueKey = chatsStateMap.remove(chatId);
        ApplicationUser commentedUser = userData.getUserByMrimLogin(mrimLogin);
        Issue commentedIssue = issueManager.getIssueByCurrentKey(issueKey);
        if (commentedUser != null && commentedIssue != null) {
            if (permissionManager.hasPermission(ProjectPermissions.ADD_COMMENTS, commentedIssue, commentedUser)) {
                commentManager.create(commentedIssue, commentedUser, commentMessage, true);
                log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated comment created...");
                icqApiClient.sendMessageText(chatId, i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.commentCreated"), null);
                log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated new comment created message queued...");
            } else {
                log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated permissions violation occurred...");
                icqApiClient.sendMessageText(chatId,i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.noPermissions"), null);
                log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated not enough permissions message queued...");
            }
        }
        log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated finished...");
    }

    public void showDefaultMenu(Chat chat, User from) throws IOException, UnirestException {
        log.debug("JiraMessageQueueProcessor showDefaultMenu started...");
        String chatId = chat.getChatId();
        ApplicationUser currentUser = userData.getUserByMrimLogin(from.getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            List<List<InlineKeyboardMarkupButton>> buttons =  messageFormatter.getMenuButtons(locale);
            icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.mainMenu.text"), buttons);
        }
        log.debug("JiraMessageQueueProcessor showDefaultMenu finished...");
    }

    public void answerCommentButtonClick(String issueKey, String queryId, String chatId, String mrimLogin, String message) throws UnirestException, IOException {
        log.debug("ANSWER COMMENT BUTTON CLICK chatId = " + chatId);
        log.debug("JiraMessageHandler answerCommentButtonClick queue offer started...");
        icqApiClient.answerCallbackQuery(queryId, null, false, null);
        icqApiClient.sendMessageText(chatId, message, messageFormatter.getCancelButton(userData.getUserByMrimLogin(mrimLogin)));
        chatsStateMap.put(chatId, issueKey);
        log.debug("JiraMessageQueueProcessor answerCommentButtonClick queue offer finished...");
    }

    public void answerCancelButtonClick(String queryId, String chatId, String message) throws UnirestException {
        log.debug("ANSWER CANCEL BUTTON CLICK chatId = " + chatId);
        log.debug("JiraMessageHandler answerCancelButtonClick queue offer started...");
        icqApiClient.answerCallbackQuery(queryId, message, false, null);
        chatsStateMap.remove(chatId);
        log.debug("JiraMessageQueueProcessor answerCancelButtonClick queue offer finished...");
    }

    public void answerQuickViewButtonClick(String issueKey, String queryId, String chatId, String mrimLogin) throws UnirestException, IOException {
        log.debug("JiraMessageHandler answerQuickViewButtonClick queue offer started...");
        icqApiClient.answerCallbackQuery(queryId, null, false, null);
        Issue currentIssue = issueManager.getIssueByCurrentKey(issueKey);
        ApplicationUser currentUser = userData.getUserByMrimLogin(mrimLogin);
        if (currentUser != null && currentIssue != null) {
            if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, currentIssue, currentUser)) {
                icqApiClient.sendMessageText(chatId, messageFormatter.createIssueSummary(currentIssue, currentUser), messageFormatter.getIssueButtons(issueKey, currentUser));
                log.debug("JiraMessageQueueProcessor answerQuickViewButtonClick show issue quick view message queued...");
            } else {
                icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"), null);
                log.debug("JiraMessageQueueProcessor answerQuickViewButtonClick no permissions message queued...");
            }
        }
    }

    public void handleNewMessageEvent(NewMessageEvent newMessageEvent) throws IOException, UnirestException {
        String chatId = newMessageEvent.getChat().getChatId();
        if (chatsStateMap.containsKey(chatId)) {
            // here we come if current new message is our new jira comment
            handleNewJiraCommentCreated(chatId, newMessageEvent.getFrom().getUserId(), newMessageEvent.getText());
        } else {
            //todo показать кнопки с выбором таски
            //showDefaultMenu(newMessageEvent.getChat(), newMessageEvent.getFrom());
        }
    }

    public void handleCallbackQueryEvent(CallbackQueryEvent callbackQueryEvent) throws IOException, UnirestException {
        String callbackData = callbackQueryEvent.getCallbackData();
        if (callbackData.startsWith("view")) {
            String issueKey = StringUtils.substringAfter(callbackData, "-");
            answerQuickViewButtonClick(issueKey,
                                       callbackQueryEvent.getQueryId(),
                                       callbackQueryEvent.getMessage().getChat().getChatId(),
                                       callbackQueryEvent.getFrom().getUserId());
        } else if (callbackData.startsWith("comment")) {
            Locale locale = localeManager.getLocaleFor(userData.getUserByMrimLogin(callbackQueryEvent.getFrom().getUserId()));
            String issueKey = StringUtils.substringAfter(callbackData, "-");
            answerCommentButtonClick(issueKey,
                                     callbackQueryEvent.getQueryId(),
                                     callbackQueryEvent.getMessage().getChat().getChatId(),
                                     callbackQueryEvent.getFrom().getUserId(),
                                     i18nResolver.getText(locale, "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.insertComment.message", issueKey));
        } else if (callbackData.startsWith("cancel")) {
            Locale locale = localeManager.getLocaleFor(userData.getUserByMrimLogin(callbackQueryEvent.getFrom().getUserId()));
            answerCancelButtonClick(callbackQueryEvent.getQueryId(),
                                    callbackQueryEvent.getMessage().getChat().getChatId(),
                                    i18nResolver.getText(locale, "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.cancelComment.message"));
        }
    }
}
