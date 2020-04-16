package ru.mail.jira.plugins.mrimsender.protocol;

import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public class JiraJobsQueueProcessor implements InitializingBean, DisposableBean {
    private static final String THREAD_NAME_PREFIX = "icq-bot-thread-pool-v2";

    private final ConcurrentLinkedQueue<JiraJob> jobsQueue = new ConcurrentLinkedQueue<>();
    private final IcqBotChatHandlers icqBotChatHandlers;
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

    public JiraJobsQueueProcessor(IcqBotChatHandlers icqBotChatHandlers) {
        this.icqBotChatHandlers = icqBotChatHandlers;
    }

    @Override
    public void destroy() {
        executorService.shutdown();
    }

    @Override
    public void afterPropertiesSet() {
        executorService.scheduleWithFixedDelay(this::processJobs, 1, 500, TimeUnit.MILLISECONDS);
    }

    private void processJobs() {
        JiraJob job;
        while ((job = jobsQueue.poll()) != null) {
            job.work.accept(job.target);
        }
    }

    public void offerNewMessageEvent(NewMessageEvent newMessageEvent) {
        jobsQueue.offer(new JiraJob(event -> {
            try {
                icqBotChatHandlers.handleNewMessageEvent((NewMessageEvent)event);
            } catch (IOException | UnirestException e) {
                log.error("An error occurred during jira job execution", e);
            }
        }, newMessageEvent));
    }

    public void offerCallbackQueryEvent(CallbackQueryEvent callbackQueryEvent) {
        jobsQueue.offer(new JiraJob(event -> {
            try {
                icqBotChatHandlers.handleCallbackQueryEvent((CallbackQueryEvent)event);
            } catch (IOException | UnirestException e) {
                log.error("An error occurred during jira job execution", e);
            }
        }, callbackQueryEvent));
    }

    @Getter
    @Setter
    public class JiraJob {
        private Consumer<Event> work;
        private Event target;

        public JiraJob(Consumer<Event> work, Event target) {
            this.work = work;
            this.target = target;
        }
    }
}
