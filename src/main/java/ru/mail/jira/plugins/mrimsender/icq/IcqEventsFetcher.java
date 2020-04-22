package ru.mail.jira.plugins.mrimsender.icq;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.mrimsender.icq.dto.FetchResponseDto;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.IcqEventsListener;
import ru.mail.jira.plugins.mrimsender.protocol.events.CancelClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.CommentIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.SearchIssueClickEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ViewIssueClickEvent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class IcqEventsFetcher {
    private static final Logger log = LoggerFactory.getLogger(IcqEventsFetcher.class);
    private static final String THREAD_NAME_PREFIX_FORMAT = "icq-events-fetcher-thread-pool-%d";
    private final IcqApiClient icqApiClient;
    private final IcqEventsListener icqEventsListener;
    private AtomicBoolean isRunning;
    private ScheduledExecutorService fetcherExecutorService;
    private ScheduledFuture<?> currentFetchJobFuture;
    private long lastEventId = 0;


    public IcqEventsFetcher(IcqApiClient icqApiClient, IcqEventsListener icqEventsListener) {
        isRunning = new AtomicBoolean(false);
        this.icqApiClient = icqApiClient;
        this.icqEventsListener = icqEventsListener;
    }

    public void start() {
        log.debug("IcqEventsFetcher starting ...");
        if (isRunning.compareAndSet(false, true)) {
            fetcherExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_PREFIX_FORMAT).build());
            currentFetchJobFuture = fetcherExecutorService.scheduleWithFixedDelay(() -> {
                try {
                    this.fetchIcqEvents();
                } catch (Exception e) {
                    log.error("An exception occurred inside fetcher executor service job", e);
                }
            }, 0, 1, TimeUnit.SECONDS);
            log.debug("IcqEventsFetcher started");
        }
    }

    public void fetchIcqEvents() {
        try {
            log.debug(String.format("IcqEventsFetcher fetch icq events started  lastEventId=%d...", lastEventId));
            HttpResponse<FetchResponseDto> httpResponse = icqApiClient.getEvents(lastEventId, 15);
            if (httpResponse.getStatus() == 200) {
                log.debug("IcqEventsFetcher handle icq events started ...");
                AtomicLong eventId = new AtomicLong(lastEventId);
                httpResponse.getBody()
                            .getEvents()
                            .forEach(event -> {
                                if (event instanceof NewMessageEvent) {
                                    eventId.set(event.getEventId());
                                    icqEventsListener.publishIcqNewMessageEvent((NewMessageEvent)event);
                                } else if (event instanceof CallbackQueryEvent) {
                                    eventId.set(event.getEventId());
                                    CallbackQueryEvent callbackQueryEvent = (CallbackQueryEvent)event;
                                    String buttonPrefix = StringUtils.substringBefore(callbackQueryEvent.getCallbackData(), "-");
                                    if (buttonPrefix.equals("view")) {
                                        icqEventsListener.postIcqButtonClickEvent(new ViewIssueClickEvent(callbackQueryEvent));
                                    } else if (buttonPrefix.equals("comment")) {
                                        icqEventsListener.postIcqButtonClickEvent(new CommentIssueClickEvent(callbackQueryEvent));
                                    } else if (buttonPrefix.equals("cancel")) {
                                        icqEventsListener.postIcqButtonClickEvent(new CancelClickEvent(callbackQueryEvent));
                                    } else if (buttonPrefix.equals("search")) {
                                        icqEventsListener.postIcqButtonClickEvent(new SearchIssueClickEvent(callbackQueryEvent));
                                    }
                                } else {
                                    eventId.set(event.getEventId());
                                }
                            });

                this.lastEventId = eventId.get();
            }
            log.debug("IcqEventsFetcher fetchIcqEvents finished.... ");
        } catch (UnirestException e) {
            log.error("unirest exception occurred", e);
            // exception occurred during events fetching, for example http connection timeout
        }

    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            currentFetchJobFuture.cancel(true);
            fetcherExecutorService.shutdownNow();
        }
    }

    public AtomicBoolean getIsRunning() {
        return this.isRunning;
    }
}
