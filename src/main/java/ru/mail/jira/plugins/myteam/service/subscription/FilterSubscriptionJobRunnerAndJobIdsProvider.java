/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.subscription;

import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.service.FilterSubscriptionService;
import ru.mail.jira.plugins.myteam.service.JobRunnerAndJobIdsProvider;

@Component
public class FilterSubscriptionJobRunnerAndJobIdsProvider extends JobRunnerAndJobIdsProvider {

  public FilterSubscriptionJobRunnerAndJobIdsProvider(
      final FilterSubscriptionService filterSubscriptionService,
      final SendMyteamSubscriptionNotificationsJobRunner
          sendMyteamSubscriptionNotificationsJobRunner) {
    super(
        filterSubscriptionService::getAllFilterSubscriptionJobIds,
        sendMyteamSubscriptionNotificationsJobRunner);
  }
}
