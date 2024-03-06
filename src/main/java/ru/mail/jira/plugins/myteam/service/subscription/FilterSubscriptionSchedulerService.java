/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.subscription;

import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.Schedule;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.commons.Const;
import ru.mail.jira.plugins.myteam.service.FilterSubscriptionService;

@Component
public class FilterSubscriptionSchedulerService {

  private final SchedulerService schedulerService;

  @Autowired
  public FilterSubscriptionSchedulerService(
      @ComponentImport final SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  public void createScheduleJob(final int subscriptionId, @NotNull final String cronExpression) {
    try {
      final Schedule schedule = Schedule.forCronExpression(cronExpression);
      final JobConfig config = getJobConfig(subscriptionId, schedule);
      schedulerService.scheduleJob(getJobId(subscriptionId), config);
    } catch (final SchedulerServiceException e) {
      SentryClient.capture(e, Map.of("subscriptionId", String.valueOf(subscriptionId)));
      throw new DataAccessException(e);
    }
  }

  public void updateScheduleJob(final int subscriptionId, @NotNull final String cronExpression) {
    try {
      final Schedule schedule = Schedule.forCronExpression(cronExpression);
      deleteScheduleJob(subscriptionId);
      final JobConfig config = getJobConfig(subscriptionId, schedule);
      schedulerService.scheduleJob(getJobId(subscriptionId), config);
    } catch (final SchedulerServiceException e) {
      SentryClient.capture(e, Map.of("subscriptionId", String.valueOf(subscriptionId)));
      throw new DataAccessException(e);
    }
  }

  public void deleteScheduleJob(final int subscriptionId) {
    final JobId jobId = getJobId(subscriptionId);
    if (schedulerService.getJobDetails(jobId) != null) {
      try {
        schedulerService.unscheduleJob(jobId);
      } catch (Exception e) {
        SentryClient.capture(e, Map.of("subscriptionId", String.valueOf(subscriptionId)));
      }
    } else {
      SentryClient.capture(
          String.format(
              "Unable to find a scheduled job for myteam subscription schedule: %d. Removing the schedule anyway.",
              subscriptionId));
    }
  }

  public static JobConfig getJobConfig(final int subscriptionId, final Schedule schedule) {
    return JobConfig.forJobRunnerKey(FilterSubscriptionService.JOB_RUNNER_KEY)
        .withSchedule(schedule)
        .withParameters(Map.of(Const.SCHEDULE_ID, subscriptionId));
  }

  public static JobId getJobId(final int subscriptionId) {
    return JobId.of(FilterSubscriptionService.class.getName() + ':' + subscriptionId);
  }
}
