/* (C)2024 */
package ru.mail.jira.plugins.myteam.service;

import com.atlassian.jira.util.lang.Pair;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import java.util.List;
import java.util.function.Supplier;
import lombok.Getter;

public abstract class JobRunnerAndJobIdsProvider {

  private final Supplier<List<Pair<JobId, JobConfig>>> jobIdsProvider;

  @Getter private final ExtendedJobRunner extendedJobRunner;

  public JobRunnerAndJobIdsProvider(
      final Supplier<List<Pair<JobId, JobConfig>>> jobIdsProvider,
      final ExtendedJobRunner extendedJobRunner) {
    this.jobIdsProvider = jobIdsProvider;
    this.extendedJobRunner = extendedJobRunner;
  }

  public List<Pair<JobId, JobConfig>> getJobIdsAndJobConfigs() {
    return jobIdsProvider.get();
  }
}
