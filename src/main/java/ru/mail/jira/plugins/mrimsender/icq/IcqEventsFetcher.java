package ru.mail.jira.plugins.mrimsender.icq;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.mrimsender.icq.dto.FetchResponseDto;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.BotFaultToleranceProvider;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class IcqEventsFetcher {
    private static final Logger log = LoggerFactory.getLogger(BotFaultToleranceProvider.class);
    private static final String THREAD_NAME_PREFIX_FORMAT = "icq-events-fetcher-thread-pool-%d";
    private final IcqEventsHandler icqEventsHandler;

    // default api value
    private long lastEventId = 0;
    private AtomicBoolean isRunning;
    private ExecutorService fetcherExecutorService;
    private volatile Future<?> currentFetchJobFuture;
    private IcqApiClient icqApiClient;


    public IcqEventsFetcher(IcqApiClient icqApiClient, IcqEventsHandler icqEventsHandler) {
        isRunning = new AtomicBoolean(false);
        this.icqEventsHandler = icqEventsHandler;
        this.icqApiClient = icqApiClient;
    }

    public void start() {
        log.debug("IcqEventsFetcher starting ...");
        if (isRunning.compareAndSet(false, true)) {
            fetcherExecutorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_PREFIX_FORMAT).build());
            currentFetchJobFuture = fetcherExecutorService.submit(() -> this.executeFetch(lastEventId));
            log.debug("IcqEventsFetcher started");
        }
    }

    public void executeFetch(long lastEventId) {
        log.debug("IcqEventsFetcher execute fetch started ...");
        if (isRunning.get())
            currentFetchJobFuture = CompletableFuture.supplyAsync(this::fetchIcqEvents, fetcherExecutorService)
                                                     .thenAcceptAsync((FetchResponseDto fetchResponseDto) -> this.executeFetch(lastEventId), fetcherExecutorService);
    }

    public FetchResponseDto fetchIcqEvents() {
        try {
            log.debug("IcqEventsFetcher fetch icq events started ...");
            HttpResponse<FetchResponseDto> httpResponse = icqApiClient.getEvents(lastEventId, 60);
            this.handle(httpResponse);
            log.debug("IcqEventsFetcher fetchIcqEvents finished.... ");
            return httpResponse.getBody();
        } catch (UnirestException e) {
            // exception occurred during events fetching, for example http connection timeout
            return this.fetchIcqEvents();
        }
    }

    public void handle(HttpResponse<FetchResponseDto> httpResponse) {
        if (httpResponse.getStatus() != 200)
            return;
        log.debug("IcqEventsFetcher handle icq events started ...");
        httpResponse.getBody()
                    .getEvents()
                    .forEach(event -> {
                        if (event instanceof NewMessageEvent) {
                            icqEventsHandler.handleEvent((NewMessageEvent) event);
                        } else if (event instanceof CallbackQueryEvent) {
                            icqEventsHandler.handleEvent((CallbackQueryEvent) event);
                        }
                    });
        log.debug("IcqEventsFetcher handling icq events finished ...");
        int eventsNum = httpResponse.getBody().getEvents().size();
        this.lastEventId = httpResponse.getBody().getEvents().get(eventsNum - 1).getEventId();
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
