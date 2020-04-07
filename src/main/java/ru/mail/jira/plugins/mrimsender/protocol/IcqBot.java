package ru.mail.jira.plugins.mrimsender.protocol;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IcqBot implements InitializingBean, DisposableBean {
    private static final Logger log = Logger.getLogger(IcqBot.class);
    private static final String BOT_SEND_URL = "https://api.icq.net/bot/v1";
    private static final String THREAD_NAME_PREFIX = "icq-bot-thread-pool";

    private final PluginData pluginData;
    private final IcqApiClient icqApiClient;
    private final ConcurrentLinkedQueue<Pair<String, String>> queue = new ConcurrentLinkedQueue<>();
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

    private volatile String botToken;

    public IcqBot(PluginData pluginData, IcqApiClient icqApiClient) {
        this.pluginData = pluginData;
        this.icqApiClient = icqApiClient;
    }

    public void sendMessage(String mrimLogin, String message) {
        queue.offer(Pair.of(mrimLogin, message));
    }

    public void initToken() {
        this.botToken = pluginData.getToken();
    }

    @Override
    public void destroy() {
        executorService.shutdown();
    }

    @Override
    public void afterPropertiesSet() {
        initToken();
        executorService.scheduleWithFixedDelay(this::processMessages, 1, 500, TimeUnit.MILLISECONDS);
    }

    private void processMessages() {
        Pair<String, String> msg;
        while ((msg = queue.poll()) != null) {
            try {
                icqApiClient.sendMessageText(msg.getKey(), msg.getValue(), null);
            } catch (UnirestException | IOException e) {
                //e.printStackTrace();
            }
        }
    }

    private String encodeFormParam(String key, String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
    }
}
