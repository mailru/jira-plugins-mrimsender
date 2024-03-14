/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.subscription;

import com.atlassian.jira.util.lang.Pair;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.service.ExtendedJobRunner;
import ru.mail.jira.plugins.myteam.service.JobRunnerAndJobIdsProvider;

@Component
@ExportAsService
public class JobRunnerRegistrar implements LifecycleAware, DisposableBean {
  private final SchedulerService schedulerService;
  private final List<JobRunnerAndJobIdsProvider> jobIdProviders;

  @Autowired
  public JobRunnerRegistrar(
      @ComponentImport final SchedulerService schedulerService,
      final List<JobRunnerAndJobIdsProvider> jobIdProviders) {
    this.schedulerService = schedulerService;
    this.jobIdProviders = jobIdProviders;
  }

  @Override
  public void onStart() {
    jobIdProviders.stream().forEach(this::tryRegisterAndScheduleJobRunner);
  }

  @Override
  public void onStop() {}

  @Override
  public void destroy() throws Exception {
    jobIdProviders.forEach(this::tryUnscheduleJobs);
  }

  private void tryRegisterAndScheduleJobRunner(
      final JobRunnerAndJobIdsProvider jobRunnerAndJobIdsProvider) {
    try {
      final ExtendedJobRunner extendedJobRunner = jobRunnerAndJobIdsProvider.getExtendedJobRunner();
      schedulerService.registerJobRunner(
          extendedJobRunner.getJobRunnerKey(), jobRunnerAndJobIdsProvider.getExtendedJobRunner());
      final List<String> failedStartScheduleJobId = new ArrayList<>();
      for (final Pair<JobId, JobConfig> jobIdInfo :
          jobRunnerAndJobIdsProvider.getJobIdsAndJobConfigs()) {
        try {
          schedulerService.scheduleJob(jobIdInfo.first(), jobIdInfo.second());
        } catch (Exception e) {
          failedStartScheduleJobId.add(jobIdInfo.first().toString());
        }
      }

      if (failedStartScheduleJobId.size() != 0) {
        SentryClient.capture(String.join(",", failedStartScheduleJobId));
      }
    } catch (Exception e) {
      SentryClient.capture(
          e,
          null,
          Map.of(
              "jobRunnerKey",
              jobRunnerAndJobIdsProvider.getExtendedJobRunner().getJobRunnerKey().toString()));
    }
  }

  private void tryUnscheduleJobs(final JobRunnerAndJobIdsProvider jobIdProvider) {
    try {
      final List<String> failedUnscheduledJobIds = new ArrayList<>();
      for (final Pair<JobId, JobConfig> jobId : jobIdProvider.getJobIdsAndJobConfigs()) {
        try {
          schedulerService.unscheduleJob(jobId.first());
        } catch (Exception e) {
          failedUnscheduledJobIds.add(jobId.toString());
        }
      }

      if (failedUnscheduledJobIds.size() != 0) {
        SentryClient.capture(String.join(",", failedUnscheduledJobIds));
      }

      tryUnregisterJobRunner(jobIdProvider.getExtendedJobRunner());
    } catch (final Exception e) {
      SentryClient.capture(
          e, null, Map.of("JobIdProviderClass", jobIdProvider.getClass().toString()));
    }
  }

  private void tryUnregisterJobRunner(final ExtendedJobRunner extendedJobRunner) {
    try {
      schedulerService.unregisterJobRunner(extendedJobRunner.getJobRunnerKey());
    } catch (final Exception e) {
      SentryClient.capture(
          e, null, Map.of("jobRunnerKey", extendedJobRunner.getJobRunnerKey().toString()));
    }
  }
}
