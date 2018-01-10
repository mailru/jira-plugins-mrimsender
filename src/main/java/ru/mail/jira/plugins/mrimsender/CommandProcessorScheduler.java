package ru.mail.jira.plugins.mrimsender;

import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.atlassian.scheduler.config.RunMode;
import com.atlassian.scheduler.config.Schedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import ru.mail.jira.plugins.mrimsender.protocol.CommandProcessor;

import java.util.Calendar;

public class CommandProcessorScheduler implements LifecycleAware, DisposableBean {
    private final static Logger log = LoggerFactory.getLogger(CommandProcessorScheduler.class);

    private static final JobRunnerKey JOB_RESTART_RUNNER_KEY = JobRunnerKey.of(CommandProcessorScheduler.class.getName() + "-RESTART");
    private static final JobRunnerKey JOB_STOP_RUNNER_KEY = JobRunnerKey.of(CommandProcessorScheduler.class.getName() + "-STOP");
    private static final JobId FIRST_START_JOB_ID = JobId.of(CommandProcessorScheduler.class.getName() + "-FIRST_START");
    private static final JobId RESTART_JOB_ID = JobId.of(CommandProcessorScheduler.class.getName() + "-RESTART");
    private static final JobId STOP_JOB_ID = JobId.of(CommandProcessorScheduler.class.getName() + "-STOP");

    private final SchedulerService schedulerService;

    public CommandProcessorScheduler(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Override
    public void onStart() {
        try {
            schedulerService.registerJobRunner(JOB_RESTART_RUNNER_KEY, new JobRunner() {
                @Override
                public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
                    try {
                        CommandProcessor.restart();
                        return JobRunnerResponse.success();
                    } catch (Exception e) {
                        log.error("Error while trying to run command processor", e);
                        return JobRunnerResponse.failed(e);
                    }
                }
            });
            JobConfig jobStartConfig = JobConfig.forJobRunnerKey(JOB_RESTART_RUNNER_KEY)
                                                .withSchedule(Schedule.forCronExpression("0 0 0 1/1 * ? *"))
                                                .withRunMode(RunMode.RUN_ONCE_PER_CLUSTER);
            schedulerService.scheduleJob(RESTART_JOB_ID, jobStartConfig);

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, 5);

            schedulerService.scheduleJob(FIRST_START_JOB_ID, JobConfig.forJobRunnerKey(JOB_RESTART_RUNNER_KEY)
                                                                      .withSchedule(Schedule.runOnce(calendar.getTime()))
                                                                      .withRunMode(RunMode.RUN_ONCE_PER_CLUSTER));

            schedulerService.registerJobRunner(JOB_STOP_RUNNER_KEY, new JobRunner() {
                @Override
                public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
                    try {
                        CommandProcessor.shutdown();
                        return JobRunnerResponse.success();
                    } catch (Exception e) {
                        log.error("Error while trying to stop command processor", e);
                        return JobRunnerResponse.failed(e);
                    }
                }
            });
            JobConfig jobStopConfig = JobConfig.forJobRunnerKey(JOB_STOP_RUNNER_KEY)
                                               .withSchedule(Schedule.forCronExpression("0 59 23 1/1 * ? *"))
                                               .withRunMode(RunMode.RUN_LOCALLY);
            schedulerService.scheduleJob(STOP_JOB_ID, jobStopConfig);
        } catch (Exception e) {
            log.error("Error while initializing CommandProcessorScheduler jobs", e);
        }
    }

    @Override
    public void onStop() {

    }

    @Override
    public void destroy() throws Exception {
        schedulerService.unscheduleJob(RESTART_JOB_ID);
        schedulerService.unregisterJobRunner(JOB_RESTART_RUNNER_KEY);
        schedulerService.unscheduleJob(STOP_JOB_ID);
        schedulerService.unregisterJobRunner(JOB_STOP_RUNNER_KEY);
    }
}
