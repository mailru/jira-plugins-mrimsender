package ru.mail.jira.plugins.calendar.schedule;

import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.calendar.common.CalendarUtils;
import ru.mail.jira.plugins.calendar.schedule.model.Schedule;
import ru.mail.jira.plugins.calendar.schedule.service.ScheduleService;

public class MailRuCalendarDeleteSchedule extends MailRuCalendarAbstractScheduleAction {
    private static final String SECURITY_BREACH = "securitybreach";

    private final ScheduleService scheduleService;

    public MailRuCalendarDeleteSchedule(
            @ComponentImport AvatarService avatarService,
            @ComponentImport IssueManager issueManager,
            ScheduleService scheduleService,
            CalendarUtils calendarUtils
    ) {
        super(avatarService, issueManager, calendarUtils);
        this.scheduleService = scheduleService;
    }

    @Override
    public String doDefault() throws Exception {
        if (getLoggedInUser() == null)
            return SECURITY_BREACH;

        Schedule schedule = scheduleService.getSchedule(getId());
        if (schedule == null) {
            addErrorMessage(getText("ru.mail.jira.plugins.calendar.schedule.notExist"));
            return ERROR;
        }
        this.setName(schedule.getName());

        if (!scheduleService.hasPermissionToEditAndDelete(schedule, getLoggedInUser()))
            return SECURITY_BREACH;

        return super.doDefault();
    }

    @Override
    protected void doValidation() {
    }

    @Override
    @RequiresXsrfCheck
    public String doExecute() throws Exception {
        if (getLoggedInUser() == null)
            return SECURITY_BREACH;

        Schedule schedule = scheduleService.getSchedule(getId());
        if (!scheduleService.hasPermissionToEditAndDelete(schedule, getLoggedInUser()))
            return SECURITY_BREACH;

        scheduleService.deleteSchedule(getId());

        if (isInlineDialogMode())
            return returnComplete();
        return getRedirect("secure/MailRuCalendarSchedulesConfigurationPage.jspa");
    }
}
