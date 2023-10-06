/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.bot.events.*;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.myteam.myteam.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.myteam.myteam.dto.response.FetchResponse;

@Component
public class MyteamEventsFetcher {
  private static final Logger log = LoggerFactory.getLogger(MyteamEventsFetcher.class);
  private static final String THREAD_NAME_PREFIX_FORMAT = "icq-events-fetcher-%d";
  private final MyteamApiClient myteamApiClient;
  private final MyteamEventsListener myteamEventsListener;
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  @Nullable private ScheduledExecutorService fetcherExecutorService;
  @Nullable private ScheduledFuture<?> currentFetchJobFuture;
  private long lastEventId = 0;

  @Autowired
  public MyteamEventsFetcher(
      MyteamApiClient myteamApiClient, MyteamEventsListener myteamEventsListener) {
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
              500,
              TimeUnit.MILLISECONDS);
      log.debug("IcqEventsFetcher started");
    }
  }

  public void fetchIcqEvents() {
    try {
      log.debug("IcqEventsFetcher fetch icq events started  lastEventId={}...", lastEventId);
      HttpResponse<FetchResponse> httpResponse = myteamApiClient.getEvents(lastEventId, 2);
      if (httpResponse.getStatus() == 200) {
        log.debug("IcqEventsFetcher handle icq events started ...");
        // TODO зачем тут атомик ? forEach же не параллельный ...
        AtomicLong eventId = new AtomicLong(lastEventId);
        Optional.ofNullable(httpResponse.getBody())
            .map(FetchResponse::getEvents)
            .ifPresent(
                icqEvents ->
                    icqEvents.forEach(
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
                                event,
                                e);
                          } finally {
                            eventId.set(event.getEventId());
                          }
                        }));
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
      if (currentFetchJobFuture != null) {
        currentFetchJobFuture.cancel(true);
      }
      if (fetcherExecutorService != null) {
        fetcherExecutorService.shutdownNow();
      }
    }
  }

  public AtomicBoolean getIsRunning() {
    return this.isRunning;
  }

  public void resetLastEventId() {
    this.lastEventId = 0;
  }
}
