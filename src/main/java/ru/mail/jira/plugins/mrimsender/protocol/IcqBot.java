package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.cluster.ClusterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.IcqEventsFetcher;

import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class IcqBot implements InitializingBean, DisposableBean {
    private final IcqApiClient icqApiClient;
    private final IcqEventsFetcher icqEventsFetcher;
    private final ReentrantLock startLock = new ReentrantLock();
    private final ClusterManager clusterManager;

    private volatile boolean isRespondingBot = false;

    public IcqBot(ClusterManager clusterManager, IcqApiClient icqApiClient, IcqEventsFetcher icqEventsFetcher) {
        this.clusterManager = clusterManager;
        this.icqApiClient = icqApiClient;
        this.icqEventsFetcher = icqEventsFetcher;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (!clusterManager.isClustered())
            this.startRespondingBot();
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
            this.icqApiClient.updateToken();
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
