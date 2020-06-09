package ru.mail.jira.plugins.mrimsender.protocol;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.IcqEventsFetcher;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class IcqBot implements DisposableBean {
    private final IcqApiClient icqApiClient;
    private final IcqEventsFetcher icqEventsFetcher;
    private final ReentrantLock startLock = new ReentrantLock();

    private volatile boolean isRespondingBot = false;

    public IcqBot(IcqApiClient icqApiClient, IcqEventsFetcher icqEventsFetcher) {
        this.icqApiClient = icqApiClient;
        this.icqEventsFetcher = icqEventsFetcher;
    }


    @Override
    public void destroy() throws Exception {
        if (icqEventsFetcher.getIsRunning().get())
            icqEventsFetcher.stop();
    }


    public void startRespondingBot() {
        if (startLock.tryLock()) {
            try {
                icqEventsFetcher.start();
                isRespondingBot = true;
            } finally {
                startLock.unlock();
            }
        } else {
            log.debug("Icq bot fetcher didn't started, because it was locked on start");
        }
    }

    public void stopBot() {
        if (isRespondingBot && icqEventsFetcher.getIsRunning().get()) {
            startLock.lock();
            try {
                icqEventsFetcher.stop();
            } finally {
                startLock.unlock();
            }
        } else {
            log.debug("Icq bot didn't stopped, because fetcher wasn't started");
        }
    }

    public void restartBot() {
        startLock.lock();
        try {
            this.icqApiClient.updateSettings();
            if (isRespondingBot) {
                this.icqEventsFetcher.stop();
                this.icqEventsFetcher.start();
            }
        }  finally {
            startLock.unlock();
        }
    }

    public boolean isFetcherRunning() {
        return this.icqEventsFetcher.getIsRunning().get();
    }
}
