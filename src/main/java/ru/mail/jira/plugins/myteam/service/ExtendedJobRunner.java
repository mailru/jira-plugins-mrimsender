/* (C)2024 */
package ru.mail.jira.plugins.myteam.service;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.config.JobRunnerKey;

public interface ExtendedJobRunner extends JobRunner {

  JobRunnerKey getJobRunnerKey();
}
