package ru.mail.jira.plugins.calendar.schedule;

import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.calendar.common.CalendarUtils;
import ru.mail.jira.plugins.calendar.schedule.service.ScheduleService;

public class MailRuCalendarCreateSchedule extends MailRuCalendarAbstractScheduleAction {
    private static final String SECURITY_BREACH = "securitybreach";

    private final ScheduleService scheduleService;

    public MailRuCalendarCreateSchedule(
            @ComponentImport AvatarService avatarService,
            @ComponentImport IssueManager issueManager,
            CalendarUtils calendarUtils,
            ScheduleService scheduleService
    ) {
        super(avatarService, issueManager, calendarUtils);
        this.scheduleService = scheduleService;
    }

    @Override
    public String doDefault() throws Exception {
        if (getLoggedInUser() == null)
            return SECURITY_BREACH;
        return INPUT;
    }

    @Override
    protected void doValidation() {
        super.doValidation();
    }

    @Override
    @RequiresXsrfCheck
    public String doExecute() throws Exception {
        if (getLoggedInUser() == null)
            return SECURITY_BREACH;
        scheduleService.createSchedule(getIssueId(), getName(), getSchedule(), buildCronEditorBeanParams());

        if (isInlineDialogMode())
            return returnComplete();
        return getRedirect("/browse/" + getIssue().getKey());
    }
}