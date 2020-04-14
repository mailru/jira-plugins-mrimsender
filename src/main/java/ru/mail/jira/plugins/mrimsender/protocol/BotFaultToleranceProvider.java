package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.jira.cluster.ClusterManager;
import com.atlassian.jira.cluster.Node;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.atlassian.scheduler.config.RunMode;
import com.atlassian.scheduler.config.Schedule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;

import javax.annotation.Nullable;
import java.util.concurrent.locks.Lock;

import static com.atlassian.jira.entity.ClusterLockStatusEntity.LOCK_NAME;

public class BotFaultToleranceProvider implements LifecycleAware {
    private static final JobRunnerKey CHOOSE_MAIN_NODE_JOB = JobRunnerKey.of(BotFaultToleranceProvider.class + "- random node job");
    private static final JobId CHOOSE_MAIN_NODE_JOB_ID = JobId.of(BotFaultToleranceProvider.class.getName() + "- choose main node");

    private static final JobRunnerKey CHECK_BOT_WORKS_JOB = JobRunnerKey.of(BotFaultToleranceProvider.class + "- global cluster job");
    private static final JobId CHECK_BOT_WORKS_JOB_ID = JobId.of(BotFaultToleranceProvider.class.getName() + "- check bot works");
    private static final Logger log = LoggerFactory.getLogger(BotFaultToleranceProvider.class);

    //jira services managers data ... etc
    private final SchedulerService schedulerService;
    private final ClusterManager clusterManager;
    private final TimeZoneManager timeZoneManager;
    private final Lock clusterLock;
    private final IcqBot icqBot;

    //this plugin services managers data ... etc
    private final PluginData pluginData;

    public BotFaultToleranceProvider(SchedulerService schedulerService,
                                     ClusterManager clusterManager,
                                     TimeZoneManager timeZoneManager,
                                     ClusterLockService clusterLockService,
                                     PluginData pluginData,
                                     IcqBot icqBot) {
        this.schedulerService = schedulerService;
        this.clusterManager = clusterManager;
        this.timeZoneManager = timeZoneManager;
        this.pluginData = pluginData;
        this.clusterLock = clusterLockService.getLockForName(LOCK_NAME);
        this.icqBot = icqBot;
    }

    private JobConfig registerChooseMainNodeJob() {
        //global job settings
        schedulerService.registerJobRunner(CHOOSE_MAIN_NODE_JOB, new JobRunner() {
            @Nullable
            @Override
            public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
                log.debug("MAIN JOB CHECKING");
                if (pluginData.getToken() == null) {
                    //this case happens when admin still didn't set bot properties
                    log.debug("TOKEN NOT FOUND");
                    return JobRunnerResponse.failed("token not found");
                }
                clusterLock.lock();
                try {
                    if (StringUtils.isNotBlank(pluginData.getMainNodeId())) {
                        //there is information about main node let's checks if this node is alive
                        //checking if any main node is running
                        for (Node node : clusterManager.findLiveNodes()) {
                            if (node.getNodeId() != null && node.getNodeId().equals(pluginData.getMainNodeId())) {
                                //so main node is alive => we shouldn't do anything
                                log.debug("there is information about main node");
                                return JobRunnerResponse.success();
                            }
                         }
                        //if we are here, then it means that main node is down, so now we are a main node
                        pluginData.setMainNodeId(clusterManager.getNodeId());
                        //DO NOT USE RESTART HERE
                        log.debug("Find that main node is down, become a main node");
                        icqBot.stopBot();
                        icqBot.startRespondingBot();
                        log.debug("bot was started in main node mode");
                    } else {
                        //there is no information about main node (must happen just after plugin start)
                        //than we are the main node
                        pluginData.setMainNodeId(clusterManager.getNodeId());
                        icqBot.stopBot();
                        icqBot.startRespondingBot();
                        log.debug("main node info is empty so we set main node number, and restarted bot");
                        log.debug("ClusterManager node id = {}, Main node id from plugin data = {}", clusterManager.getNodeId(), pluginData.getMainNodeId());
                    }
                } finally {
                    clusterLock.unlock();
                }
                return JobRunnerResponse.success();
            }
        });
        return JobConfig.forJobRunnerKey(CHOOSE_MAIN_NODE_JOB)
                        .withSchedule(Schedule.forCronExpression("0 0/1 * ? * *", timeZoneManager.getDefaultTimezone()))
                        .withRunMode(RunMode.RUN_ONCE_PER_CLUSTER);
    }

    private JobConfig registerCheckBotWorksJob() {
        //local job settings
        schedulerService.registerJobRunner(CHECK_BOT_WORKS_JOB, new JobRunner() {
            @Nullable
            @Override
            public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
                if (!clusterManager.isClustered() && !icqBot.isFetcherRunning() && pluginData.getToken() != null) {
                    // this is server instance, restart bot if it was turned off
                    log.debug("Found out that current bot session isn't running, so the session was restarted");
                    icqBot.startRespondingBot();
                }
                if (clusterManager.isClustered()) {
                    // data center
                    clusterLock.lock();
                    try {
                        log.debug("LOCAL JOB CHECKING");
                        //checks if any main node started
                        if (pluginData.getMainNodeId() == null) {
                            log.debug("there is no main node id found");
                            return JobRunnerResponse.failed("There is no main node number set");
                        }
                        //checks if we are a main node
                        if (pluginData.getMainNodeId().equals(clusterManager.getNodeId())) {
                            log.debug("Main node id == current node id");
                            //checks our bot service is alive and didn't break because of telegram RLE for example
                            if (!icqBot.isFetcherRunning()) {
                                log.debug("Found out that current bot session isn't running, so restart the session");
                                icqBot.startRespondingBot();
                            }
                        }
                    } finally {
                        clusterLock.unlock();
                    }
                }
                return JobRunnerResponse.success();
            }
        });

        return JobConfig.forJobRunnerKey(CHECK_BOT_WORKS_JOB)
                        .withSchedule(Schedule.forCronExpression("30 0/1 * ? * *", timeZoneManager.getDefaultTimezone()))
                        .withRunMode(RunMode.RUN_LOCALLY);
    }

    @Override
    public void onStart() {
        try {
            if (clusterManager.isClustered()) {
                JobConfig chooseMainNodeJobConf = registerChooseMainNodeJob();
                schedulerService.scheduleJob(CHOOSE_MAIN_NODE_JOB_ID, chooseMainNodeJobConf);
            }
            JobConfig checkBotWorksJobConf = registerCheckBotWorksJob();
            schedulerService.scheduleJob(CHECK_BOT_WORKS_JOB_ID, checkBotWorksJobConf);
        } catch (SchedulerServiceException e) {
            log.debug("Scheduler service exception was catched: ", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {
        if (clusterManager.isClustered()) {
            if (clusterManager.getNodeId() != null && clusterManager.getNodeId().equals(pluginData.getMainNodeId()))
                pluginData.setMainNodeId(null);
            if (clusterManager.findLiveNodes().size() == 0) {
                // if there is no live nodes, then we are the last one which is turned off
                // => unschedule and unregister choose main node job
                schedulerService.unscheduleJob(CHOOSE_MAIN_NODE_JOB_ID);
                schedulerService.unregisterJobRunner(CHOOSE_MAIN_NODE_JOB);
            }
        }
        schedulerService.unscheduleJob(CHECK_BOT_WORKS_JOB_ID);
        schedulerService.unregisterJobRunner(CHECK_BOT_WORKS_JOB);
    }
}
