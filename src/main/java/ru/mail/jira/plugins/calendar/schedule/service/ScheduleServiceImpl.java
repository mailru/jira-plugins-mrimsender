package ru.mail.jira.plugins.calendar.schedule.service;

import com.atlassian.jira.bc.issue.CloneIssueCommand;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.task.TaskDescriptor;
import com.atlassian.jira.task.TaskManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.web.component.cron.CronEditorBean;
import com.atlassian.jira.web.component.cron.generator.CronExpressionGenerator;
import com.atlassian.jira.web.component.cron.parser.CronExpressionParser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.common.CalendarUtils;
import ru.mail.jira.plugins.calendar.common.Consts;
import ru.mail.jira.plugins.calendar.schedule.model.Schedule;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ScheduleServiceImpl implements ScheduleService {
    private static final JobRunnerKey JOB_RUNNER_KEY = JobRunnerKey.of(ScheduleServiceImpl.class.getName());

    private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);
    private TaskDescriptor<CloneIssueCommand.CloneIssueResult> currentTaskDescriptor;

    private final CalendarUtils calendarUtils;
    private final CustomFieldManager customFieldManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final IssueManager issueManager;
    private final IssueService issueService;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final ScheduleManager scheduleManager;
    private final SchedulerService schedulerService;
    private final TaskManager taskManager;
    private final UserManager userManager;

    @Autowired
    public ScheduleServiceImpl(
            @ComponentImport CustomFieldManager customFieldManager,
            @ComponentImport GlobalPermissionManager globalPermissionManager,
            @ComponentImport IssueManager issueManager, IssueService issueService,
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
            @ComponentImport SchedulerService schedulerService,
            @ComponentImport TaskManager taskManager,
            @ComponentImport UserManager userManager,
            CalendarUtils calendarUtils,
            ScheduleManager scheduleManager
        ) {
        this.calendarUtils = calendarUtils;
        this.customFieldManager = customFieldManager;
        this.globalPermissionManager = globalPermissionManager;
        this.issueManager = issueManager;
        this.issueService = issueService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.scheduleManager = scheduleManager;
        this.schedulerService = schedulerService;
        this.taskManager = taskManager;
        this.userManager = userManager;
        schedulerService.registerJobRunner(JOB_RUNNER_KEY, new CloneIssueJob());
    }

    private static JobConfig getJobConfig(final int scheduleId, final com.atlassian.scheduler.config.Schedule schedule) {
        return JobConfig.forJobRunnerKey(JOB_RUNNER_KEY)
                        .withSchedule(schedule)
                        .withParameters(ImmutableMap.<String, Serializable>of(Consts.SCHEDULE_ID, scheduleId));
    }

    private Map<CustomField, Optional<Boolean>> getCloneOptionSelections(List<CustomField> customFields, Issue issue) {
        Map<CustomField, Optional<Boolean>> cloneOptionSelections = Maps.newHashMap();
        for(CustomField cf : customFields) {
            Optional<Boolean> cloneOptionSelection;
            cloneOptionSelection = Optional.empty();
            cloneOptionSelections.put(cf, cloneOptionSelection);
        }
        return cloneOptionSelections;
    }

    @Override
    public void createSchedule(long issueId, String name, String mode, Map<String, String[]> scheduleParams) {
        CronEditorBean cronEditorBean = new CronEditorBean(Consts.SCHEDULE_PREFIX, scheduleParams);
        CronExpressionGenerator cronExpressionGenerator = new CronExpressionGenerator();
        Schedule scheduleIssue = scheduleManager.createSchedule(issueId, name, jiraAuthenticationContext.getLoggedInUser().getKey(), mode, cronExpressionGenerator.getCronExpressionFromInput(cronEditorBean));

        try {
            com.atlassian.scheduler.config.Schedule schedule = com.atlassian.scheduler.config.Schedule.forCronExpression(scheduleIssue.getCronExpression());
            JobConfig config = getJobConfig(scheduleIssue.getID(), schedule);
            schedulerService.scheduleJob(getJobId(scheduleIssue.getID()), config);
        } catch (final SchedulerServiceException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public void updateSchedule(int id, String name, String mode, Map<String, String[]> scheduleParams) {
        CronEditorBean cronEditorBean = new CronEditorBean(Consts.SCHEDULE_PREFIX, scheduleParams);
        CronExpressionGenerator cronExpressionGenerator = new CronExpressionGenerator();
        Schedule scheduleIssue = scheduleManager.updateSchedule(id, name, mode, cronExpressionGenerator.getCronExpressionFromInput(cronEditorBean));

        try {
            com.atlassian.scheduler.config.Schedule schedule = com.atlassian.scheduler.config.Schedule.forCronExpression(scheduleIssue.getCronExpression());
            schedulerService.unscheduleJob(getJobId(scheduleIssue.getID()));
            JobConfig config = getJobConfig(scheduleIssue.getID(), schedule);
            schedulerService.scheduleJob(getJobId(scheduleIssue.getID()), config);
        } catch (final SchedulerServiceException e) {
            throw new DataAccessException(e);
        }
    }

    @Override
    public void deleteSchedule(int id) {
        JobId jobId = getJobId(id);
        if (schedulerService.getJobDetails(jobId) != null)
            schedulerService.unscheduleJob(jobId);
        else
            log.debug("Unable to find a scheduled job for the issue schedule: " + id + ". Removing the schedule anyway.");
        scheduleManager.deleteSchedule(id);
    }

    @Override
    public Schedule getSchedule(int id) {
        return scheduleManager.getSchedule(id);
    }

    @Override
    public Map<String, String[]> getScheduleParams(int id) throws Exception {
        Schedule schedule = scheduleManager.getSchedule(id);
        String scheduleMode = schedule.getMode();
        Map<String, String[]> scheduleParams = new HashMap<String, String[]>();
        scheduleParams.put("schedule", new String[] {scheduleMode});
        if (scheduleMode.equals(CronEditorBean.ADVANCED_MODE)) {
            scheduleParams.put("advanced", new String[] {schedule.getCronExpression()});
            return scheduleParams;
        }

        CronExpressionParser cronExpressionParser = new CronExpressionParser(schedule.getCronExpression());
        CronEditorBean cronEditorBean = cronExpressionParser.getCronEditorBean();
        if (cronEditorBean.getMinutes() != null && cronEditorBean.getHoursRunOnce() != null) {
            scheduleParams.put("hours", new String[] {String.valueOf(calendarUtils.get24HourTime(Integer.parseInt(cronEditorBean.getHoursRunOnce()), cronEditorBean.getHoursRunOnceMeridian()))});
            scheduleParams.put("minutes", new String[] {cronEditorBean.getMinutes()});
        }
        if (scheduleMode.equals(CronEditorBean.DAYS_OF_WEEK_SPEC_MODE) && cronEditorBean.getSpecifiedDaysPerWeek() != null)
            scheduleParams.put("weekdays", StringUtils.split(cronEditorBean.getSpecifiedDaysPerWeek(), ","));
        if (scheduleMode.equals(CronEditorBean.DAYS_OF_MONTH_SPEC_MODE) && cronEditorBean.getDayOfMonth() != null)
            scheduleParams.put("monthDay", new String[] {cronEditorBean.getDayOfMonth()});
        return scheduleParams;
    }

    @Override
    public boolean hasPermissionToEditAndDelete(Schedule schedule, ApplicationUser user) {
        return schedule.getCreatorKey().equals(user.getKey()) || globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }

    class CloneIssueJob implements JobRunner {
        @Nullable
        @Override
        public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
            final Map<String, Serializable> parameters = jobRunnerRequest.getJobConfig().getParameters();
            Integer scheduleId = (Integer) parameters.get(Consts.SCHEDULE_ID);

            Schedule scheduleIssue = scheduleManager.getSchedule(scheduleId);
            if (scheduleIssue == null)
                return JobRunnerResponse.failed("No issue schedule for id " + scheduleId);

            cloneIssue(scheduleId);
            return JobRunnerResponse.success();
        }
    }

    public void cloneIssue(int scheduleId) {
        Schedule scheduleIssue = scheduleManager.getSchedule(scheduleId);
        Issue issue = issueManager.getIssueObject(scheduleIssue.getSourceIssueId());
        ApplicationUser scheduleCreator = userManager.getUserByKey(scheduleIssue.getCreatorKey());
        List<CustomField> customFields = customFieldManager.getCustomFieldObjects(issue.getProjectId(), issue.getIssueTypeId());
        IssueService.CloneValidationResult cloneValidationResult = issueService.validateClone(scheduleCreator, issue, issue.getSummary(), true, true, true, getCloneOptionSelections(customFields, issue));
        if (cloneValidationResult.isValid()) {
            IssueService.AsynchronousTaskResult asynchronousTaskResult = issueService.clone(scheduleCreator, cloneValidationResult);
            if (asynchronousTaskResult.isValid()) {
                try {
                    Long taskId = asynchronousTaskResult.getTaskId();
                    int time = 0;
                    while (time < 10) {
                        currentTaskDescriptor = taskManager.getTask(taskId);
                        if (currentTaskDescriptor.isFinished() && !currentTaskDescriptor.isCancelled()) {
                            CloneIssueCommand.CloneIssueResult cloneIssueResult = currentTaskDescriptor.getResult();
                            if (cloneIssueResult.isSuccessful()) {
                                Issue clonedIssue = issueManager.getIssueByCurrentKey(currentTaskDescriptor.getResult().getIssueKey());
                                scheduleManager.updateSchedule(scheduleId, scheduleIssue.getRunCount() + 1, new Date(), clonedIssue.getId());
                            } else
                                for (Map.Entry<String, String> entry : cloneIssueResult.getErrorCollection().getErrors().entrySet())
                                    log.error(String.format("Error: %s\nReason: %s", entry.getKey(), entry.getValue()));
                            return;
                        }
                        Thread.sleep(1000);
                        time++;
                    }
                } catch (Exception e) {
                    log.error(String.format("Error with clone issue %s by %s", issue.getKey(), scheduleCreator.getDisplayName()), e);
                }
            } else {
                for (Map.Entry<String, String> entry : asynchronousTaskResult.getErrorCollection().getErrors().entrySet())
                    log.error(String.format("Error: %s\nReason: %s", entry.getKey(), entry.getValue()));
            }
        } else {
            for (Map.Entry<String, String> entry : cloneValidationResult.getErrorCollection().getErrors().entrySet())
                log.error(String.format("Error: %s\nReason: %s", entry.getKey(), entry.getValue()));
        }
    }

    public JobId getJobId(final int scheduleId) {
        return JobId.of(ScheduleServiceImpl.class.getName() + ':' + scheduleId);
    }
}
