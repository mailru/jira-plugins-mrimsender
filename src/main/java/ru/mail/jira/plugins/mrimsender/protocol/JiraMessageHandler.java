package ru.mail.jira.plugins.mrimsender.protocol;

import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class JiraMessageHandler implements InitializingBean, DisposableBean {
    private static final String THREAD_NAME_PREFIX = "icq-bot-thread-pool";

    private final ConcurrentLinkedQueue<Pair<JiraMessageType, JiraMessage>> queue = new ConcurrentLinkedQueue<>();
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

    public JiraMessageHandler(IcqApiClient icqApiClient) {
        this.icqApiClient = icqApiClient;
    }

    public void sendMessage(String mrimLogin, String message) {
        log.debug("JiraMessageHandler sendMessage  queue offer started...");
        queue.offer(Pair.of(JiraMessageType.MESSAGE, new JiraMessage(mrimLogin, null, message)));
        log.debug("JiraMessageHandler sendMessage queue offer finished");

    }

    public void answerButtonClick(String queryId, String message) {
        log.debug("JiraMessageHandler answerButtonClick queue offer started...");
        queue.offer(Pair.of(JiraMessageType.CALLBACK_MESSAGE, new JiraMessage(null, queryId, message)));
        log.debug("JiraMessageHandler answerButtonClick queue offer finished...");
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
        Pair<JiraMessageType, JiraMessage> msg;
        while ((msg = queue.poll()) != null) {
            try {
                switch (msg.getKey()) {
                    case MESSAGE:
                        icqApiClient.sendMessageText(msg.getValue().getMrimLogin(), msg.getValue().getText(), null);
                        break;
                    case CALLBACK_MESSAGE:
                        icqApiClient.answerCallbackQuery(msg.getValue().getQueryId(), msg.getValue().getText(), false, null);
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
        private String text;
        private String mrimLogin;
        private String queryId;

        public JiraMessage(String login, String queryId, String text) {
            this.mrimLogin = login;
            this.queryId = queryId;
            this.text = text;
        }
    }

    public enum JiraMessageType {
        MESSAGE, CALLBACK_MESSAGE;
    }
}
