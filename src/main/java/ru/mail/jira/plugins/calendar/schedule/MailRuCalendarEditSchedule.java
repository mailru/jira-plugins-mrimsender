package ru.mail.jira.plugins.calendar.schedule;

import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.calendar.common.CalendarUtils;
import ru.mail.jira.plugins.calendar.schedule.model.Schedule;
import ru.mail.jira.plugins.calendar.schedule.service.ScheduleService;

import java.util.Map;

@Scanned
public class MailRuCalendarEditSchedule extends MailRuCalendarAbstractScheduleAction {
    private static final String SECURITY_BREACH = "securitybreach";

    private final ScheduleService scheduleService;

    public MailRuCalendarEditSchedule(
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
        int scheduleId = getId();
        if (getId() != null) {
            Schedule schedule = scheduleService.getSchedule(scheduleId);
            Map<String, String[]> scheduleParams = scheduleService.getScheduleParams(scheduleId);
            this.setName(schedule.getName());
            if (scheduleParams.containsKey("schedule"))
                this.setSchedule(scheduleParams.get("schedule")[0]);
            if (scheduleParams.containsKey("hours"))
                this.setHours(Integer.parseInt(scheduleParams.get("hours")[0]));
            if (scheduleParams.containsKey("minutes"))
                this.setMinutes(Integer.parseInt(scheduleParams.get("minutes")[0]));
            if (scheduleParams.containsKey("weekdays"))
                this.setWeekdays(scheduleParams.get("weekdays"));
            if (scheduleParams.containsKey("monthDay"))
                this.setMonthDay(Integer.parseInt(scheduleParams.get("monthDay")[0]));
            if (scheduleParams.containsKey("advanced"))
                this.setAdvanced(scheduleParams.get("advanced")[0]);
        }
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
        scheduleService.updateSchedule(getId(), getName(), getSchedule(), buildCronEditorBeanParams());

        if (isInlineDialogMode())
            return returnComplete();
        return getRedirect("secure/MailRuCalendarSchedulesConfigurationPage.jspa");
    }
}
