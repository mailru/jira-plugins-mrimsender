/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.subscription;

import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.commons.Const;
import ru.mail.jira.plugins.myteam.service.ExtendedJobRunner;
import ru.mail.jira.plugins.myteam.service.FilterSubscriptionService;

@Component
public class SendMyteamSubscriptionNotificationsJobRunner implements ExtendedJobRunner {
  private final FilterSubscriptionService filterSubscriptionService;

  @Autowired
  public SendMyteamSubscriptionNotificationsJobRunner(
      final FilterSubscriptionService filterSubscriptionService) {
    this.filterSubscriptionService = filterSubscriptionService;
  }

  @Nullable
  @Override
  public JobRunnerResponse runJob(final JobRunnerRequest jobRunnerRequest) {
    final Map<String, Serializable> parameters = jobRunnerRequest.getJobConfig().getParameters();
    Integer subscriptionId = (Integer) parameters.get(Const.SCHEDULE_ID);
    if (subscriptionId == null) {
      return JobRunnerResponse.failed(
          String.format("No My Team subscription for id %d", subscriptionId));
    }

    try {
      filterSubscriptionService.runFilterSubscriptionIfUserHasPermission(subscriptionId);
      return JobRunnerResponse.success();
    } catch (Exception e) {
      SentryClient.capture(e);
      return JobRunnerResponse.failed(
          String.format(
              "Fail send notification for My Team subscription with id %d. Error: %s",
              subscriptionId, e.getMessage()));
    }
  }

  @Override
  public JobRunnerKey getJobRunnerKey() {
    return FilterSubscriptionService.JOB_RUNNER_KEY;
  }

  @Override
  public boolean isNeedScheduleOnStartPlugin() {
    return false;
  }

  @Override
  @Nullable
  public JobConfig getJobConfig() {
    return null;
  }

  @Override
  @Nullable
  public JobId getJobId() {
    return null;
  }
}
