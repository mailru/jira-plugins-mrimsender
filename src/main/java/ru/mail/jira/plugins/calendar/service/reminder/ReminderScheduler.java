package ru.mail.jira.plugins.calendar.service.reminder;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.*;
import com.atlassian.scheduler.config.*;
import com.atlassian.scheduler.status.JobDetails;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.service.PluginData;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.atlassian.scheduler.config.RunMode.RUN_ONCE_PER_CLUSTER;

@Component
@ExportAsService(com.atlassian.sal.api.lifecycle.LifecycleAware.class)
public class ReminderScheduler implements LifecycleAware, DisposableBean, JobRunner {
    private static final int MIN_DELAY = 15000;
    private static final int MAX_JITTER = 10000;

    private final Random random = new Random();
    private final JobId jobId = JobId.of("ru.mail.jira.plugins.calendar.reminder");
    private final JobRunnerKey jobRunnerKey = JobRunnerKey.of(ReminderScheduler.class.getName());

    private final Logger logger = LoggerFactory.getLogger(ReminderScheduler.class);
    private final SchedulerService schedulerService;
    private final ReminderService reminderService;
    private final PluginData pluginData;

    @Autowired
    public ReminderScheduler(
        @ComponentImport SchedulerService schedulerService,
        ReminderService reminderService,
        PluginData pluginData
    ) {
        this.schedulerService = schedulerService;
        this.reminderService = reminderService;
        this.pluginData = pluginData;
    }

    @Override
    public void onStart() {
        schedulerService.registerJobRunner(jobRunnerKey, this);

        final int jitter = random.nextInt(MAX_JITTER);
        final Date firstRun = new Date(System.currentTimeMillis() + MIN_DELAY + jitter);
        final Map<String, Serializable> parameters = ImmutableMap.of();

        final JobConfig jobConfig = JobConfig.forJobRunnerKey(jobRunnerKey)
            .withSchedule(Schedule.forInterval(TimeUnit.MINUTES.toMillis(1), firstRun))
            .withRunMode(RUN_ONCE_PER_CLUSTER)
            .withParameters(parameters);
        logger.info("Scheduling job with jitter=" + jitter + ": " + jobConfig);

        try {
            final JobDetails existing = schedulerService.getJobDetails(jobId);
            if (existing != null)
            {
                logger.info("We will be replacing an existing job with jobId=" + jobId + ": " + existing);
            }

            schedulerService.scheduleJob(jobId, jobConfig);
            logger.info("Successfully scheduled jobId=" + jobId);
        } catch (SchedulerServiceException e) {
            throw new RuntimeException("Unable to create schedule", e);
        }
    }

    @Override
    public void onStop() {
    }

    @Override
    public void destroy() throws Exception {
        logger.info("unsceduling");
        schedulerService.unscheduleJob(jobId);
        schedulerService.unregisterJobRunner(jobRunnerKey);
    }

    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
        long now = System.currentTimeMillis();
        Long lastRun = pluginData.getReminderLastRun();

        if (lastRun == null) {
            pluginData.setReminderLastRun(now);
            return JobRunnerResponse.success();
        }

        try {
            reminderService.triggerNotificationsInRange(lastRun, now);
            pluginData.setReminderLastRun(now);
            return JobRunnerResponse.success();
        } catch (Exception e) {
            logger.error("exception caught while handling reminders", e);
            return JobRunnerResponse.failed(e);
        }
    }
}
