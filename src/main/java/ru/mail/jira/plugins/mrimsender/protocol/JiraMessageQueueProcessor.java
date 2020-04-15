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
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class JiraMessageQueueProcessor implements InitializingBean, DisposableBean {
    private static final String THREAD_NAME_PREFIX = "icq-bot-thread-pool";

    private final ConcurrentHashMap<String, String> chatsStateMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<JiraMessage> queue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2, new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, THREAD_NAME_PREFIX + threadNumber.getAndIncrement());
            if (t.isDaemon())
                t.setDaemon(false);
            return t;
        }
    });
    private final IcqApiClient icqApiClient;
    private final CommentManager commentManager;
    private final UserData userData;
    private final IssueManager issueManager;
    private final MessageFormatter messageFormatter;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final PermissionManager permissionManager;

    public JiraMessageQueueProcessor(IcqApiClient icqApiClient,
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

    public void sendMessage(String chatId, String message, List<List<InlineKeyboardMarkupButton>> buttons) {
        log.debug("JiraMessageQueueProcessor sendMessage started...");
        queue.offer(new JiraMessage(JiraMessageType.MESSAGE, chatId, message, buttons));
        log.debug("JiraMessageQueueProcessor sendMessage finished...");
    }

    public void handleNewJiraCommentCreated(String chatId, String mrimLogin, String commentMessage) {
        log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated started...");
        String issueKey = chatsStateMap.remove(chatId);
        ApplicationUser commentedUser = userData.getUserByMrimLogin(mrimLogin);
        Issue commentedIssue = issueManager.getIssueByCurrentKey(issueKey);
        if (commentedUser != null && commentedIssue != null) {
            if (permissionManager.hasPermission(ProjectPermissions.ADD_COMMENTS, commentedIssue, commentedUser)) {
                commentManager.create(commentedIssue, commentedUser, commentMessage, false);
                log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated comment created...");
                queue.offer(new JiraMessage(JiraMessageType.MESSAGE, chatId, i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.commentCreated"), null));
                log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated new comment created message queued...");
            } else {
                log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated permissions violation occurred...");
                queue.offer(new JiraMessage(JiraMessageType.MESSAGE, chatId, i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.noPermissions"), null));
                log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated not enough permissions message queued...");
            }
        }
        log.debug("JiraMessageQueueProcessor handleNewJiraCommentCreated finished...");
    }

    public void answerCommentButtonClick(String issueKey, String queryId, String chatId, String message) {
        log.debug("ANSWER COMMENT BUTTON CLICK chatId = " + chatId);
        log.debug("JiraMessageHandler answerCommentButtonClick queue offer started...");
        queue.offer(new JiraMessage(JiraMessageType.CALLBACK_MESSAGE, queryId, message));
        chatsStateMap.put(chatId, issueKey);
        log.debug("JiraMessageQueueProcessor answerCommentButtonClick queue offer finished...");
    }

    public void answerQuickViewButtonClick(String issueKey, String queryId, String chatId, String mrimLogin) {
        log.debug("JiraMessageHandler answerQuickViewButtonClick queue offer started...");
        queue.offer(new JiraMessage(JiraMessageType.CALLBACK_MESSAGE, queryId, null));
        Issue currentIssue = issueManager.getIssueByCurrentKey(issueKey);
        ApplicationUser currentUser = userData.getUserByMrimLogin(mrimLogin);
        if (currentUser != null && currentIssue != null) {
            if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, currentIssue, currentUser)) {
                queue.offer(new JiraMessage(JiraMessageType.MESSAGE,
                                            chatId,
                                            messageFormatter.createIssueSummary(currentIssue, currentUser),
                                            messageFormatter.getIssueButtons(issueKey, currentUser)));
                log.debug("JiraMessageQueueProcessor answerQuickViewButtonClick show issue quick view message queued...");
            } else {
                queue.offer(new JiraMessage(JiraMessageType.MESSAGE,
                                            chatId,
                                            i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"),
                                            null));
                log.debug("JiraMessageQueueProcessor answerQuickViewButtonClick no permissions message queued...");
            }
        }
    }

    public void handleNewMessageEvent(NewMessageEvent newMessageEvent) {
        String chatId = newMessageEvent.getChat().getChatId();
        if (chatsStateMap.containsKey(chatId)) {
            // here we come if current new message is our new jira comment
            handleNewJiraCommentCreated(chatId, newMessageEvent.getFrom().getUserId(), newMessageEvent.getText());
        } else {
            //todo показать кнопки с выбором таски
        }
    }

    public void handleCallbackQueryEvent(CallbackQueryEvent callbackQueryEvent) {
        String callbackData = callbackQueryEvent.getCallbackData();
        if (callbackData.startsWith("view")) {
            String issueKey = StringUtils.substringAfter(callbackData, "-");
            answerQuickViewButtonClick(issueKey,
                                       callbackQueryEvent.getQueryId(),
                                       callbackQueryEvent.getMessage().getChat().getChatId(),
                                       callbackQueryEvent.getFrom().getUserId());
        }
        if (callbackData.startsWith("comment")) {
            Locale locale = localeManager.getLocaleFor(userData.getUserByMrimLogin(callbackQueryEvent.getFrom().getUserId()));
            String issueKey = StringUtils.substringAfter(callbackData, "-");
            answerCommentButtonClick(issueKey,
                                     callbackQueryEvent.getQueryId(),
                                     callbackQueryEvent.getMessage().getChat().getChatId(),
                                     i18nResolver.getText(locale, "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.insertComment.message", issueKey));
        }
    }

    @Override
    public void destroy() {
        executorService.shutdown();
    }

    @Override
    public void afterPropertiesSet() {
        executorService.scheduleWithFixedDelay(this::processMessages, 1, 500, TimeUnit.MILLISECONDS);
    }

    private void processMessages() {
        JiraMessage msg;
        while ((msg = queue.poll()) != null) {
            try {
                switch (msg.getMessageType()) {
                    case MESSAGE:
                        icqApiClient.sendMessageText(msg.getChatId(), msg.getText(), msg.getButtons());
                        break;
                    case CALLBACK_MESSAGE:
                        icqApiClient.answerCallbackQuery(msg.getQueryId(), msg.getText(), false, null);
                        break;
                    default:
                        break;
                }
            } catch (UnirestException | IOException e) {
                log.error("An error occurred during icq api message sending", e);
            }
        }
    }

    public enum JiraMessageType {
        MESSAGE, CALLBACK_MESSAGE;
    }

    @Setter
    @Getter
    public class JiraMessage {
        private JiraMessageType messageType;
        private String text;
        private String chatId;
        private String queryId;
        private List<List<InlineKeyboardMarkupButton>> buttons;

        public JiraMessage(JiraMessageType messageType, String queryId, String text) {
            this.messageType = messageType;
            this.queryId = queryId;
            this.text = text;
        }

        public JiraMessage(JiraMessageType jiraMessageType, String chatId, String text, List<List<InlineKeyboardMarkupButton>> buttons) {
            this.messageType = jiraMessageType;
            this.chatId = chatId;
            this.text = text;
            this.buttons = buttons;
        }
    }
}
