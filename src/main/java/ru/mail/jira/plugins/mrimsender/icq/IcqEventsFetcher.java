package ru.mail.jira.plugins.mrimsender.icq;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.icq.dto.FetchResponseDto;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class IcqEventsFetcher {
    private final PluginData pluginData;

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
        this.pluginData = pluginData;
        this.icqApiClient = new IcqApiClientImpl(this.pluginData);
    }

    public void start() {
        fetcherExecutorService = Executors.newSingleThreadExecutor();
        if (isRunning.compareAndSet(false, true)) {
            currentFetchJobFuture = fetcherExecutorService.submit(this::fetchIcqEvents);
        }
    }

    public void fetchIcqEvents() {
        try {
            String result = (String)currentFetchJobFuture.get();
            HttpResponse<FetchResponseDto> httpResponse = icqApiClient.getEvents(lastEventId, 60);
            this.handle(httpResponse);
            currentFetchJobFuture = fetcherExecutorService.submit(this::fetchIcqEvents);
        } catch (ExecutionException | UnirestException e) {
            // exception occurred during events fetching, for example http connection timeout
            currentFetchJobFuture = fetcherExecutorService.submit(this::fetchIcqEvents);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
            if (event.getType().equals(Consts.EventType.DELETED_MESSAGE_TYPE.getTypeStrValue())) {
                handlersMap.get(Consts.EventType.DELETED_MESSAGE_TYPE).accept(event);
            }
            if (event.getType().equals(Consts.EventType.EDITED_MESSAGE_TYPE.getTypeStrValue())) {
                handlersMap.get(Consts.EventType.EDITED_MESSAGE_TYPE).accept(event);
            }
            if (event.getType().equals(Consts.EventType.LEFT_CHAT_MEMBERS_TYPE.getTypeStrValue())) {
                handlersMap.get(Consts.EventType.LEFT_CHAT_MEMBERS_TYPE).accept(event);
            }
            if (event.getType().equals(Consts.EventType.NEW_CHAT_MEMBERS_TYPE.getTypeStrValue())) {
                handlersMap.get(Consts.EventType.NEW_CHAT_MEMBERS_TYPE).accept(event);
            }
            if (event.getType().equals(Consts.EventType.PINNED_MESSAGE_TYPE.getTypeStrValue())) {
                handlersMap.get(Consts.EventType.PINNED_MESSAGE_TYPE).accept(event);
            }
            if (event.getType().equals(Consts.EventType.UNPINNED_MESSAGE_TYPE.getTypeStrValue())) {
                handlersMap.get(Consts.EventType.UNPINNED_MESSAGE_TYPE).accept(event);
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
