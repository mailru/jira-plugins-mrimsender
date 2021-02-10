/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.FetchResponseDto;
import ru.mail.jira.plugins.myteam.myteam.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.myteam.myteam.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.*;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.protocol.listeners.MyteamEventsListener;

@Component
public class MyteamEventsFetcher {
  private static final Logger log = LoggerFactory.getLogger(MyteamEventsFetcher.class);
  private static final String THREAD_NAME_PREFIX_FORMAT = "icq-events-fetcher-thread-pool-%d";
  private final MyteamApiClient myteamApiClient;
  private final MyteamEventsListener myteamEventsListener;
  private AtomicBoolean isRunning;
  private ScheduledExecutorService fetcherExecutorService;
  private ScheduledFuture<?> currentFetchJobFuture;
  private long lastEventId = 0;

  @Autowired
  public MyteamEventsFetcher(
      MyteamApiClient myteamApiClient, MyteamEventsListener myteamEventsListener) {
    isRunning = new AtomicBoolean(false);
    this.myteamApiClient = myteamApiClient;
    this.myteamEventsListener = myteamEventsListener;
  }

  public void start() {
    log.debug("IcqEventsFetcher starting ...");
    if (isRunning.compareAndSet(false, true)) {
      fetcherExecutorService =
          Executors.newSingleThreadScheduledExecutor(
              new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_PREFIX_FORMAT).build());
      currentFetchJobFuture =
          fetcherExecutorService.scheduleWithFixedDelay(
              () -> {
                try {
                  this.fetchIcqEvents();
                } catch (Exception e) {
                  log.error("An exception occurred inside fetcher executor service job", e);
                }
              },
              0,
              1,
              TimeUnit.SECONDS);
      log.debug("IcqEventsFetcher started");
    }
  }

  public void fetchIcqEvents() {
    try {
      log.debug("IcqEventsFetcher fetch icq events started  lastEventId={}...", lastEventId);
      HttpResponse<FetchResponseDto> httpResponse = myteamApiClient.getEvents(lastEventId, 15);
      if (httpResponse.getStatus() == 200) {
        log.debug("IcqEventsFetcher handle icq events started ...");
        // TODO зачем тут атомик ? forEach же не параллельный ...
        AtomicLong eventId = new AtomicLong(lastEventId);
        httpResponse
            .getBody()
            .getEvents()
            .forEach(
                event -> {
                  try {
                    if (event instanceof NewMessageEvent) {
                      myteamEventsListener.publishEvent(
                          new ChatMessageEvent((NewMessageEvent) event));
                    }
                    if (event instanceof CallbackQueryEvent) {
                      myteamEventsListener.publishEvent(
                          new ButtonClickEvent((CallbackQueryEvent) event));
                    }
                  } catch (Exception e) {
                    log.error(
                        "Exception inside fetchIcqEvents occurred with event = {}",
                        event.toString(),
                        e);
                  } finally {
                    eventId.set(event.getEventId());
                  }
                });
        this.lastEventId = eventId.get();
      }
      log.debug("IcqEventsFetcher fetchIcqEvents finished.... ");
    } catch (UnirestException | MyteamServerErrorException e) {
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

  public void resetLastEventId() {
    this.lastEventId = 0;
  }
}
