/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.subscription;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.SchedulerService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.service.ExtendedJobRunner;

@Component
public class JobRunnerRegistrar implements LifecycleAware, DisposableBean {
  private final SchedulerService schedulerService;
  private final List<ExtendedJobRunner> extendedJobRunners;

  @Autowired
  public JobRunnerRegistrar(
      @ComponentImport final SchedulerService schedulerService,
      final List<ExtendedJobRunner> extendedJobRunners) {
    this.schedulerService = schedulerService;
    this.extendedJobRunners = Collections.unmodifiableList(extendedJobRunners);
  }

  @Override
  public void onStop() {}

  @Override
  public void destroy() throws Exception {
    extendedJobRunners.forEach(this::tryUnregisterJobRunner);
  }

  @Override
  public void onStart() {
    extendedJobRunners.forEach(this::tryRegisterAndScheduleJobRunner);
  }

  private void tryRegisterAndScheduleJobRunner(final ExtendedJobRunner extendedJobRunner) {
    try {
      schedulerService.registerJobRunner(extendedJobRunner.getJobRunnerKey(), extendedJobRunner);
      if (extendedJobRunner.isNeedScheduleOnStartPlugin()
          && extendedJobRunner.getJobId() != null
          && extendedJobRunner.getJobConfig() != null) {
        schedulerService.scheduleJob(
            extendedJobRunner.getJobId(), extendedJobRunner.getJobConfig());
      }
    } catch (Exception e) {
      SentryClient.capture(
          e, null, Map.of("jobRunnerKey", extendedJobRunner.getJobRunnerKey().toString()));
    }
  }

  private void tryUnregisterJobRunner(final ExtendedJobRunner extendedJobRunner) {
    try {
      schedulerService.unregisterJobRunner(extendedJobRunner.getJobRunnerKey());
    } catch (Exception e) {
      SentryClient.capture(
          e, null, Map.of("jobRunnerKey", extendedJobRunner.getJobRunnerKey().toString()));
    }
  }
}
