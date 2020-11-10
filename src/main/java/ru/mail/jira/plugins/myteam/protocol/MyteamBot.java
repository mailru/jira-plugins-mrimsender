/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.MyteamEventsFetcher;

@Slf4j
@Component
public class MyteamBot implements DisposableBean {
  private final MyteamApiClient myteamApiClient;
  private final MyteamEventsFetcher myteamEventsFetcher;
  private final ReentrantLock startLock = new ReentrantLock();

  private volatile boolean isRespondingBot = false;

  @Autowired
  public MyteamBot(MyteamApiClient myteamApiClient, MyteamEventsFetcher myteamEventsFetcher) {
    this.myteamApiClient = myteamApiClient;
    this.myteamEventsFetcher = myteamEventsFetcher;
  }

  @Override
  public void destroy() throws Exception {
    if (myteamEventsFetcher.getIsRunning().get()) myteamEventsFetcher.stop();
  }

  public void startRespondingBot() {
    if (startLock.tryLock()) {
      try {
        myteamEventsFetcher.start();
        isRespondingBot = true;
      } finally {
        startLock.unlock();
      }
    } else {
      log.debug("Icq bot fetcher didn't started, because it was locked on start");
    }
  }

  public void stopBot() {
    if (isRespondingBot && myteamEventsFetcher.getIsRunning().get()) {
      startLock.lock();
      try {
        myteamEventsFetcher.stop();
      } finally {
        startLock.unlock();
      }
    } else {
      log.debug("Icq bot didn't stopped, because fetcher wasn't started");
    }
  }

  public void restartBot(boolean didSettingsChanged) {
    startLock.lock();
    try {
      if (isRespondingBot) {
        this.myteamEventsFetcher.stop();
        if (didSettingsChanged) {
          this.myteamApiClient.updateSettings();
          this.myteamEventsFetcher.resetLastEventId();
        }
        this.myteamEventsFetcher.start();
      } else {
        if (didSettingsChanged) this.myteamApiClient.updateSettings();
      }
    } finally {
      startLock.unlock();
    }
  }

  public boolean isFetcherRunning() {
    return this.myteamEventsFetcher.getIsRunning().get();
  }
}
