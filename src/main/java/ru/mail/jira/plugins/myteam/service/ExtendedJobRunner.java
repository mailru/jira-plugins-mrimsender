/* (C)2024 */
package ru.mail.jira.plugins.myteam.service;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import org.jetbrains.annotations.Nullable;

public interface ExtendedJobRunner extends JobRunner {

  JobRunnerKey getJobRunnerKey();

  boolean isNeedScheduleOnStartPlugin();

  @Nullable
  JobConfig getJobConfig();

  @Nullable
  JobId getJobId();
}
