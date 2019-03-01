package ru.mail.jira.plugins.calendar.schedule.rest;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.component.cron.CronEditorBean;
import com.atlassian.jira.web.component.cron.generator.CronExpressionDescriptor;
import com.atlassian.jira.web.component.cron.parser.CronExpressionParser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.config.JobId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.common.CalendarUtils;
import ru.mail.jira.plugins.calendar.schedule.model.Schedule;
import ru.mail.jira.plugins.calendar.schedule.rest.dto.IssueDto;
import ru.mail.jira.plugins.calendar.schedule.rest.dto.ScheduleDto;
import ru.mail.jira.plugins.calendar.schedule.rest.dto.UserDto;
import ru.mail.jira.plugins.calendar.schedule.service.ScheduleManager;
import ru.mail.jira.plugins.calendar.schedule.service.ScheduleService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@Path("/schedule")
@Produces({MediaType.APPLICATION_JSON})
public class RestScheduleService {
    private static final Logger log = LoggerFactory.getLogger(RestScheduleService.class);

    private final IssueManager issueManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final UserManager userManager;
    private final I18nHelper i18nHelper;
    private final CalendarUtils calendarUtils;
    private final ScheduleManager scheduleManager;
    private final ScheduleService scheduleService;
    private final SchedulerService schedulerService;

    public RestScheduleService(
        @ComponentImport IssueManager issueManager,
        @ComponentImport GlobalPermissionManager globalPermissionManager,
        @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
        @ComponentImport SchedulerService schedulerService,
        @ComponentImport UserManager userManager,
        @ComponentImport I18nHelper i18nHelper,
        CalendarUtils calendarUtils,
        ScheduleManager scheduleManager,
        ScheduleService scheduleService
    ) {
        this.calendarUtils = calendarUtils;
        this.issueManager = issueManager;
        this.globalPermissionManager = globalPermissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.i18nHelper = i18nHelper;
        this.scheduleManager = scheduleManager;
        this.scheduleService = scheduleService;
        this.schedulerService = schedulerService;
        this.userManager = userManager;
    }

    private boolean isJiraAdmin(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }

    private String getScheduleDescription(String cronExpression, String mode) {
        try {
            CronExpressionParser cronExpressionParser = new CronExpressionParser(cronExpression);
            CronEditorBean cronEditorBean = cronExpressionParser.getCronEditorBean();
            cronEditorBean.setMode(mode);
            CronExpressionDescriptor cronExpressionDescriptor = new CronExpressionDescriptor(jiraAuthenticationContext.getI18nHelper());
            return cronExpressionDescriptor.getPrettySchedule(cronEditorBean);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }

    @GET
    public Response getAllSchedules() {
        return new RestExecutor<List<ScheduleDto>>() {
            @Override
            protected List<ScheduleDto> doAction() {
                List<ScheduleDto> result = new ArrayList<ScheduleDto>();
                ApplicationUser currentUser = jiraAuthenticationContext.getLoggedInUser();
                Schedule[] schedules = isJiraAdmin(currentUser) ? scheduleManager.getSchedules(false) : scheduleManager.getSchedules(currentUser.getKey(), false);
                for (Schedule schedule : schedules) {
                    Issue sourceIssue = issueManager.getIssueObject(schedule.getSourceIssueId());
                    Issue lastCreatedIssue = issueManager.getIssueObject(schedule.getLastCreatedIssueId());
                    ApplicationUser creator = userManager.getUserByKey(schedule.getCreatorKey());
                    result.add(new ScheduleDto(
                        schedule.getID(),
                        sourceIssue != null ?
                            new IssueDto(sourceIssue.getId(), sourceIssue.getKey(), sourceIssue.getSummary()) :
                            new IssueDto(schedule.getSourceIssueId(), i18nHelper.getText("ru.mail.jira.plugins.calendar.schedule.deletedIssueShort"), i18nHelper.getText("ru.mail.jira.plugins.calendar.schedule.deletedIssue")),
                        schedule.getName(), getScheduleDescription(schedule.getCronExpression(), schedule.getMode()),
                        new UserDto(creator.getKey(), creator.getName(), creator.getDisplayName()),
                        schedule.getMode(), schedule.getCronExpression(), schedule.getRunCount(),
                        calendarUtils.getFormattedDateTime(currentUser, schedule.getLastRun()),
                        lastCreatedIssue != null ? new IssueDto(lastCreatedIssue.getId(), lastCreatedIssue.getKey(), lastCreatedIssue.getSummary()) : null,
                        schedule.isDeleted()));
                }
                return result;
            }
        }.getResponse();
    }

    @GET
    @Path("/delete")
    public Response deleteAllSchedules() {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                scheduleManager.deleteSchedules();
                return null;
            }
        }.getResponse();
    }

    @GET
    @Path("/clone/{id}")
    public Response runCloneIssue(@PathParam("id") final int id) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                scheduleService.cloneIssue(id);
                return null;
            }
        }.getResponse();
    }

    @DELETE
    @Path("/job/{id}")
    public Response unscheduleJob(@PathParam("id") final int id) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                JobId jobId = scheduleService.getJobId(id);
                if (schedulerService.getJobDetails(jobId) != null)
                    schedulerService.unscheduleJob(jobId);
                return null;
            }
        }.getResponse();
    }
}
