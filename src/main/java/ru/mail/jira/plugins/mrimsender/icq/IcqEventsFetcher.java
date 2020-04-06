package ru.mail.jira.plugins.mrimsender.icq;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.icq.dto.FetchResponseDto;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class IcqEventsFetcher {

    // default api value
    private long lastEventId = 0;
    private AtomicBoolean isRunning;
    private ExecutorService fetcherExecutorService;
    private volatile Future<?> currentFetchJobFuture;

    private IcqApiClient icqApiClient;
    // TODO still didn't initialized
    private Map<Consts.EventType, Consumer<Event<?>>> handlersMap;

    public IcqEventsFetcher(PluginData pluginData) {
        isRunning = new AtomicBoolean(false);
        this.icqApiClient = new IcqApiClientImpl(pluginData);
    }

    public void start() {
        fetcherExecutorService = Executors.newSingleThreadExecutor();
        if (isRunning.compareAndSet(false, true)) {
            currentFetchJobFuture = fetcherExecutorService.submit(() -> this.executeFetch(lastEventId));
        }
    }

    public void executeFetch(long lastEventId) {
        if (isRunning.get())
            currentFetchJobFuture = CompletableFuture.supplyAsync(this::fetchIcqEvents, fetcherExecutorService)
                                                     .thenAcceptAsync((FetchResponseDto fetchResponseDto) -> this.executeFetch(lastEventId), fetcherExecutorService);
    }


    public FetchResponseDto fetchIcqEvents() {
        try {
            HttpResponse<FetchResponseDto> httpResponse = icqApiClient.getEvents(lastEventId, 60);
            this.handle(httpResponse);
            return httpResponse.getBody();
        } catch (UnirestException e) {
            // exception occurred during events fetching, for example http connection timeout
            return this.fetchIcqEvents();
        }
    }

    public void handle(HttpResponse<FetchResponseDto> httpResponse) {
        if (httpResponse.getStatus() != 200) return;
        httpResponse.getBody().getEvents().forEach(event -> {
            if (event.getType().equals(Consts.EventType.NEW_MESSAGE_TYPE.getTypeStrValue())) {
                handlersMap.get(Consts.EventType.NEW_MESSAGE_TYPE).accept(event);
            }
            if (event.getType().equals(Consts.EventType.CALLBACK_QUERY_TYPE.getTypeStrValue())) {
                handlersMap.get(Consts.EventType.CALLBACK_QUERY_TYPE).accept(event);
            }
        });
        int eventsNum = httpResponse.getBody().getEvents().size();
        this.lastEventId = httpResponse.getBody().getEvents().get(eventsNum - 1).getEventId();
    }

    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            currentFetchJobFuture.cancel(true);
            fetcherExecutorService.shutdownNow();
        }
    }
}
