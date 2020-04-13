package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.user.ApplicationUser;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

import java.io.IOException;

import java.util.List;
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

    public JiraMessageQueueProcessor(IcqApiClient icqApiClient, CommentManager commentManager, UserData userData, IssueManager issueManager, MessageFormatter messageFormatter) {
        this.commentManager = commentManager;
        this.userData = userData;
        this.issueManager = issueManager;
        this.icqApiClient = icqApiClient;
        this.messageFormatter = messageFormatter;
    }

    public void sendMessage(String chatId, String message, List<List<InlineKeyboardMarkupButton>> buttons) {
        log.debug("JiraMessageQueueProcessor sendMessage  queue offer started...");
        queue.offer(new JiraMessage(JiraMessageType.MESSAGE, chatId, message, buttons));
        log.debug("JiraMessageQueueProcessor sendMessage  queue offer finished...");
    }

    public void handleNewJiraCommentCreated(String chatId, String mrimLogin, String commentMessage) {
        log.debug("JiraMessageQueueProcessor createIssue comment process started...");
        String issueKey = chatsStateMap.get(chatId);
        chatsStateMap.remove(chatId);
        ApplicationUser commentedUser = userData.getUserByMrimLogin(mrimLogin);
        if (commentedUser != null) {
            commentManager.create(issueManager.getIssueByCurrentKey(issueKey), commentedUser , commentMessage,false);
            log.debug("JiraMessageQueueProcessor sendMessage queue offer started...");
            queue.offer(new JiraMessage(JiraMessageType.MESSAGE, chatId,"Comment successfully created", null));
            log.debug("JiraMessageQueueProcessor sendMessage queue offer finished...");
        }
    }

    public void answerCommentButtonClick(String issueKey, String queryId, String toggleMessage, String chatId, String message) {
        log.debug("ANSWER COMMENT BUTTON CLICK chatId = " + chatId);
        log.debug("JiraMessageHandler answerCommentButtonClick queue offer started...");
        queue.offer(new JiraMessage(JiraMessageType.CALLBACK_MESSAGE, queryId, toggleMessage));
        queue.offer(new JiraMessage(JiraMessageType.MESSAGE, chatId, message, null));
        chatsStateMap.put(chatId, issueKey);
        log.debug("JiraMessageQueueProcessor answerCommentButtonClick queue offer finished...");
    }

    public void answerButtonClick(String queryId, String toggleMessage, String chatId, String message) {
        log.debug("JiraMessageHandler answerButtonClick queue offer started...");
        queue.offer(new JiraMessage(JiraMessageType.CALLBACK_MESSAGE, queryId, toggleMessage));
        queue.offer(new JiraMessage(JiraMessageType.MESSAGE, chatId, message, null));
        log.debug("JiraMessageQueueProcessor answerButtonClick queue offer finished...");
    }

    public void answerQuickViewButtonClick(String issueKey, String queryId, String toggleMessage, String chatId, String mrimLogin) {
        log.debug("JiraMessageHandler answerQuickViewButtonClick queue offer started...");
        queue.offer(new JiraMessage(JiraMessageType.CALLBACK_MESSAGE, queryId, toggleMessage));
        queue.offer(new JiraMessage(JiraMessageType.MESSAGE, chatId, messageFormatter.createIssueSummary(issueManager.getIssueByCurrentKey(issueKey), userData.getUserByMrimLogin(mrimLogin)), null));
        log.debug("JiraMessageQueueProcessor answerQuickViewButtonClick queue offer finished...");
    }

    public void handleNewMessageEvent(NewMessageEvent newMessageEvent) {
        String chatId = newMessageEvent.getChat().getChatId();
        if (chatsStateMap.containsKey(chatId)) {
            // here we come if current new message is our new jira comment
            handleNewJiraCommentCreated(chatId, newMessageEvent.getFrom().getUserId(), newMessageEvent.getText());
        } else {
            sendMessage(chatId, "New user message event handled", null);
        }
    }

    public void handleCallbackQueryEvent(CallbackQueryEvent callbackQueryEvent) {
        String callbackData = callbackQueryEvent.getCallbackData();
        if (callbackData.startsWith("view")) {
            String issueKey = callbackData.substring(callbackData.indexOf('-') + 1);
            answerQuickViewButtonClick(issueKey, callbackQueryEvent.getQueryId(), "Quick View button clicked", callbackQueryEvent.getMessage().getChat().getChatId(), callbackQueryEvent.getFrom().getUserId());
        }
        else if (callbackData.startsWith("comment")) {
            String issueKey = callbackData.substring(callbackData.indexOf('-') + 1);
            answerCommentButtonClick(issueKey, callbackQueryEvent.getQueryId(), "Comment issue button was clicked", callbackQueryEvent.getMessage().getChat().getChatId(), "Type comment text below in next message");
        } else {
            answerButtonClick(callbackQueryEvent.getQueryId(), "Button clicked event handled", callbackQueryEvent.getMessage().getChat().getChatId(), "Button click handled");
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

    public enum JiraMessageType {
        MESSAGE, CALLBACK_MESSAGE;
    }
}
