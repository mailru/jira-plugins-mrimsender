package ru.mail.jira.plugins.calendar.schedule.service;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.*;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder;
import com.atlassian.jira.issue.link.RemoteIssueLinkManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.web.component.cron.CronEditorBean;
import com.atlassian.jira.web.component.cron.generator.CronExpressionGenerator;
import com.atlassian.jira.web.component.cron.parser.CronExpressionParser;
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
import org.apache.commons.collections.CollectionUtils;
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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ScheduleServiceImpl implements ScheduleService {
    private static final JobRunnerKey JOB_RUNNER_KEY = JobRunnerKey.of(ScheduleServiceImpl.class.getName());

    private static final Logger log = LoggerFactory.getLogger(ScheduleServiceImpl.class);

    private final ApplicationProperties applicationProperties;
    private final AttachmentManager attachmentManager;
    private final CalendarUtils calendarUtils;
    private final CustomFieldManager customFieldManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final IssueFactory issueFactory;
    private final IssueManager issueManager;
    private final IssueLinkManager issueLinkManager;
    private final IssueLinkTypeManager issueLinkTypeManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final RemoteIssueLinkManager remoteIssueLinkManager;
    private final ScheduleManager scheduleManager;
    private final SchedulerService schedulerService;
    private final UserManager userManager;
    private final PermissionManager permissionManager;

    @Autowired
    public ScheduleServiceImpl(
        @ComponentImport ApplicationProperties applicationProperties,
        @ComponentImport AttachmentManager attachmentManager,
        @ComponentImport CustomFieldManager customFieldManager,
        @ComponentImport GlobalPermissionManager globalPermissionManager,
        @ComponentImport IssueFactory issueFactory,
        @ComponentImport IssueManager issueManager,
        @ComponentImport IssueLinkManager issueLinkManager,
        @ComponentImport IssueLinkTypeManager issueLinkTypeManager,
        @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
        @ComponentImport RemoteIssueLinkManager remoteIssueLinkManager,
        @ComponentImport SchedulerService schedulerService,
        @ComponentImport UserManager userManager,
        @ComponentImport PermissionManager permissionManager,
        CalendarUtils calendarUtils,
        ScheduleManager scheduleManager
    ) {
        this.applicationProperties = applicationProperties;
        this.attachmentManager = attachmentManager;
        this.calendarUtils = calendarUtils;
        this.customFieldManager = customFieldManager;
        this.globalPermissionManager = globalPermissionManager;
        this.issueFactory = issueFactory;
        this.issueManager = issueManager;
        this.issueLinkManager = issueLinkManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.remoteIssueLinkManager = remoteIssueLinkManager;
        this.scheduleManager = scheduleManager;
        this.schedulerService = schedulerService;
        this.userManager = userManager;
        this.permissionManager = permissionManager;
        schedulerService.registerJobRunner(JOB_RUNNER_KEY, new CloneIssueJob());
    }

    private static JobConfig getJobConfig(final int scheduleId, final com.atlassian.scheduler.config.Schedule schedule) {
        return JobConfig.forJobRunnerKey(JOB_RUNNER_KEY)
                        .withSchedule(schedule)
                        .withParameters(ImmutableMap.of(Consts.SCHEDULE_ID, scheduleId));
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
            log.debug("Unable to find a scheduled job for the issue schedule: {}. Removing the schedule anyway.", id);
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
        Map<String, String[]> scheduleParams = new HashMap<>();
        scheduleParams.put("schedule", new String[]{scheduleMode});
        if (scheduleMode.equals(CronEditorBean.ADVANCED_MODE)) {
            scheduleParams.put("advanced", new String[]{schedule.getCronExpression()});
            return scheduleParams;
        }

        CronExpressionParser cronExpressionParser = new CronExpressionParser(schedule.getCronExpression());
        CronEditorBean cronEditorBean = cronExpressionParser.getCronEditorBean();
        if (cronEditorBean.getMinutes() != null && cronEditorBean.getHoursRunOnce() != null) {
            scheduleParams.put("hours", new String[]{String.valueOf(calendarUtils.get24HourTime(Integer.parseInt(cronEditorBean.getHoursRunOnce()), cronEditorBean.getHoursRunOnceMeridian()))});
            scheduleParams.put("minutes", new String[]{cronEditorBean.getMinutes()});
        }
        if (scheduleMode.equals(CronEditorBean.DAYS_OF_WEEK_SPEC_MODE) && cronEditorBean.getSpecifiedDaysPerWeek() != null)
            scheduleParams.put("weekdays", StringUtils.split(cronEditorBean.getSpecifiedDaysPerWeek(), ","));
        if (scheduleMode.equals(CronEditorBean.DAYS_OF_MONTH_SPEC_MODE) && cronEditorBean.getDayOfMonth() != null)
            scheduleParams.put("monthDay", new String[]{cronEditorBean.getDayOfMonth()});
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

    private IssueLinkType getCloneIssueLinkType() {
        IssueLinkType cloneIssueLinkType = null;
        String cloneIssueLinkTypeName = applicationProperties.getDefaultBackedString(APKeys.JIRA_CLONE_LINKTYPE_NAME);
        final Collection<IssueLinkType> cloneIssueLinkTypes = issueLinkTypeManager.getIssueLinkTypesByName(applicationProperties.getDefaultBackedString(APKeys.JIRA_CLONE_LINKTYPE_NAME));
        if (StringUtils.isNotBlank(cloneIssueLinkTypeName) && CollectionUtils.isNotEmpty(cloneIssueLinkTypes))
            for (final IssueLinkType issueLinkType : cloneIssueLinkTypes)
                if (issueLinkType.getName().equals(cloneIssueLinkTypeName))
                    cloneIssueLinkType = issueLinkType;
        return cloneIssueLinkType;
    }

    private boolean isCopyableLink(IssueLink checkingLink) {
        return !checkingLink.isSystemLink() && (getCloneIssueLinkType() == null || !getCloneIssueLinkType().getId().equals(checkingLink.getIssueLinkType().getId()));
    }

    private void cloningGivenIssueLinks(Issue cloneIssue, Collection<IssueLink> givenLinks, boolean isCopyingInwardLinks, ApplicationUser scheduleCreator) throws CreateException {
        for (final IssueLink issueLink : givenLinks)
            if (isCopyableLink(issueLink)) {
                Long workingIssueId = isCopyingInwardLinks ? issueLink.getSourceId() : issueLink.getDestinationId();
                if (isCopyingInwardLinks)
                    issueLinkManager.createIssueLink(workingIssueId, cloneIssue.getId(), issueLink.getIssueLinkType().getId(), null, scheduleCreator);
                else
                    issueLinkManager.createIssueLink(cloneIssue.getId(), workingIssueId, issueLink.getIssueLinkType().getId(), null, scheduleCreator);
            }
    }

    private void cloneRemoteIssueLinks(Issue clonedIssue, Issue originalIssue, ApplicationUser scheduleCreator) throws CreateException {
        final List<RemoteIssueLink> originalLinks = remoteIssueLinkManager.getRemoteIssueLinksForIssue(originalIssue);
        for (final RemoteIssueLink originalLink : originalLinks) {
            final RemoteIssueLink link = new RemoteIssueLinkBuilder(originalLink).id(null).issueId(clonedIssue.getId()).build();
            remoteIssueLinkManager.createRemoteIssueLink(link, scheduleCreator);
        }
    }

    private MutableIssue cloneIssue(Issue originalIssue, ApplicationUser user) {
        MutableIssue clonedIssue = issueFactory.cloneIssue(originalIssue);
        clonedIssue.setCreated(null);
        clonedIssue.setUpdated(null);
        clonedIssue.setVotes(null);
        clonedIssue.setWatches(0L);
        clonedIssue.setStatus(null);
        clonedIssue.setWorkflowId(null);
        clonedIssue.setEstimate(originalIssue.getOriginalEstimate());
        clonedIssue.setTimeSpent(null);
        clonedIssue.setResolutionDate(null);

        // If the user does not have permission to modify the reporter, initialise the reporter to be the remote user
        if (!permissionManager.hasPermission(ProjectPermissions.MODIFY_REPORTER, clonedIssue, user)) {
            clonedIssue.setReporter(user);
        }
        clonedIssue.setFixVersions(originalIssue.getFixVersions().stream().filter(version -> !version.isArchived()).collect(Collectors.toList()));
        clonedIssue.setAffectedVersions(originalIssue.getAffectedVersions().stream().filter(version -> !version.isArchived()).collect(Collectors.toList()));

        List<CustomField> customFields = customFieldManager.getCustomFieldObjects(originalIssue.getProjectId(), originalIssue.getIssueTypeId());
        for (CustomField customField : customFields) {
            Object customFieldValue = customField.getValue(originalIssue);
            if (customFieldValue != null)
                clonedIssue.setCustomFieldValue(customField, customFieldValue);
        }

        return clonedIssue;
    }

    public void cloneIssue(int scheduleId) {
        Schedule scheduleIssue = scheduleManager.getSchedule(scheduleId);
        Issue originalIssue = issueManager.getIssueObject(scheduleIssue.getSourceIssueId());
        ApplicationUser scheduleCreator = userManager.getUserByKey(scheduleIssue.getCreatorKey());

        try {
            if (originalIssue.getProjectObject() != null &&
                permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, originalIssue, scheduleCreator) &&
                permissionManager.hasPermission(ProjectPermissions.CREATE_ISSUES, originalIssue.getProjectObject(), scheduleCreator)
            ) {
                Issue createdIssue = issueManager.createIssueObject(scheduleCreator, cloneIssue(originalIssue, scheduleCreator));
                //Link with original issue
                final IssueLinkType cloneIssueLinkType = getCloneIssueLinkType();
                if (cloneIssueLinkType != null)
                    issueLinkManager.createIssueLink(createdIssue.getId(), originalIssue.getId(), cloneIssueLinkType.getId(), null, scheduleCreator);
                //Clone attachments
                if (attachmentManager.attachmentsEnabled())
                    attachmentManager.copyAttachments(originalIssue, scheduleCreator, createdIssue.getKey());
                //Clone links
                if (issueLinkManager.isLinkingEnabled()) {
                    cloningGivenIssueLinks(createdIssue, issueLinkManager.getInwardLinks(originalIssue.getId()), true, scheduleCreator);
                    cloningGivenIssueLinks(createdIssue, issueLinkManager.getOutwardLinks(originalIssue.getId()), false, scheduleCreator);
                    cloneRemoteIssueLinks(createdIssue, originalIssue, scheduleCreator);
                }
                scheduleManager.updateSchedule(scheduleId, scheduleIssue.getRunCount() + 1, new Date(), createdIssue.getId());
            } else {
                log.error("user {} don't have permission to clone issue {}", scheduleCreator, originalIssue.getKey());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public JobId getJobId(final int scheduleId) {
        return JobId.of(ScheduleServiceImpl.class.getName() + ':' + scheduleId);
    }
}
