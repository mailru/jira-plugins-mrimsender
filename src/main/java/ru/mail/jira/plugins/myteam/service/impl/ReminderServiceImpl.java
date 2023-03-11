/* (C)2023 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.config.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.component.ReminderGenerator;
import ru.mail.jira.plugins.myteam.controller.dto.ReminderDto;
import ru.mail.jira.plugins.myteam.db.model.Reminder;
import ru.mail.jira.plugins.myteam.db.repository.ReminderRepository;
import ru.mail.jira.plugins.myteam.service.ReminderService;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Component
@ExportAsService
public class ReminderServiceImpl implements LifecycleAware, DisposableBean, ReminderService {
  private static final JobRunnerKey JOB_RUNNER_KEY =
      JobRunnerKey.of(ReminderService.class.getName());
  private static final JobId JOB_ID = JobId.of(ReminderService.class.getName());

  private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

  private final SchedulerService schedulerService;
  private final TimeZoneManager timeZoneManager;
  private final UserChatService userChatService;
  private final MessageFormatter messageFormatter;
  private final ReminderRepository reminderRepository;
  private final IssueService issueService;

  public ReminderServiceImpl(
      @ComponentImport SchedulerService schedulerService,
      @ComponentImport TimeZoneManager timeZoneManager,
      @ComponentImport IssueService issueService,
      UserChatService userChatService,
      MessageFormatter messageFormatter,
      ReminderRepository reminderRepository) {
    this.schedulerService = schedulerService;
    this.timeZoneManager = timeZoneManager;
    this.userChatService = userChatService;
    this.messageFormatter = messageFormatter;
    this.reminderRepository = reminderRepository;
    this.issueService = issueService;
  }

  @Override
  public int addReminder(ReminderDto reminder, ApplicationUser user) {
    IssueService.IssueResult res = issueService.getIssue(user, reminder.getIssueKey());
    if (!res.isValid()) {
      throw new IssuePermissionException();
    }

    reminder.setVKteamsUserId(user.getEmailAddress());
    return reminderRepository.create(reminder).getID();
  }

  @Override
  public void onStart() {
    try {
      schedulerService.registerJobRunner(
          JOB_RUNNER_KEY,
          jobRunnerRequest -> {
            try {
              Arrays.stream(
                      reminderRepository.getRemindersBeforeDate(
                          LocalDateTime.now(ZoneId.systemDefault())))
                  .forEach(this::sendMessage);
              return JobRunnerResponse.success();
            } catch (Exception e) {
              log.error("Error while trying to send reminder", e);
              return JobRunnerResponse.failed(e);
            }
          });

      JobConfig jobConfig =
          JobConfig.forJobRunnerKey(JOB_RUNNER_KEY)
              .withSchedule(
                  Schedule.forCronExpression("0 * * ? * *", timeZoneManager.getDefaultTimezone()))
              .withRunMode(RunMode.RUN_ONCE_PER_CLUSTER);
      schedulerService.scheduleJob(JOB_ID, jobConfig);
    } catch (Exception e) {
      log.error("Error while initializing event publisher for contragents", e);
    }
  }

  private void sendMessage(Reminder r) {
    String chatId = r.getVKteamsUserId();
    String issueKey = r.getIssueKey();

    IssueService.IssueResult res =
        issueService.getIssue(userChatService.getJiraUserFromUserChatId(chatId), r.getIssueKey());
    if (!res.isValid()) {
      reminderRepository.delete(r);
      return;
    }
    MutableIssue issue = res.getIssue();

    try {
      if (chatId != null) {

        userChatService.sendMessageText(
            chatId,
            ReminderGenerator.generateRandomReminder(
                issue.getSummary(),
                issueKey,
                messageFormatter.createMarkdownIssueShortLink(issueKey),
                r.getDescription()));
      }
    } catch (Exception e) {
      SentryClient.capture(
          e,
          Map.of(
              "user", chatId,
              "chatId", chatId,
              "issueKey", issueKey != null ? issueKey : StringUtils.EMPTY));
    } finally {
      reminderRepository.delete(r);
      System.out.println(r.getIssueKey());
    }
  }

  @Override
  public void onStop() {}

  @Override
  public void destroy() {
    schedulerService.unscheduleJob(JOB_ID);
    schedulerService.unregisterJobRunner(JOB_RUNNER_KEY);
  }
}
