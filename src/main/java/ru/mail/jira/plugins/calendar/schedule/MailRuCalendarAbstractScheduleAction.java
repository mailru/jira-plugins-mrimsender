package ru.mail.jira.plugins.calendar.schedule;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.component.cron.CronEditorBean;
import com.atlassian.jira.web.component.cron.parser.CronExpressionParser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.calendar.common.CalendarUtils;
import ru.mail.jira.plugins.calendar.common.Consts;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MailRuCalendarAbstractScheduleAction extends JiraWebActionSupport {
    private Integer id = null;
    private String name = "";
    private long issueId;
    private String schedule;
    private Integer hours;
    private Integer minutes;
    private String[] weekdays;
    private Integer monthDay;
    private String advanced = "";

    private final AvatarService avatarService;
    private final CalendarUtils calendarUtils;
    private final IssueManager issueManager;

    public MailRuCalendarAbstractScheduleAction(
            @ComponentImport AvatarService avatarService,
            @ComponentImport IssueManager issueManager,
            CalendarUtils calendarUtils
    ) {
        this.avatarService = avatarService;
        this.calendarUtils = calendarUtils;
        this.issueManager = issueManager;
    }

    @Override
    protected void doValidation() {
        if (StringUtils.isBlank(name))
            addError("calendar-schedule-name", getText("issue.field.required", getText("ru.mail.jira.plugins.calendar.schedule.name")));
        if (!schedule.equals(CronEditorBean.ADVANCED_MODE) && (hours == null || minutes == null))
            addError("calendar-schedule-interval-time", getText("issue.field.required", getText("common.words.time")));
        if (schedule.equals(CronEditorBean.DAYS_OF_WEEK_SPEC_MODE) && weekdays == null)
            addError("calendar-schedule-interval-weekdays", getText("issue.field.required", getText("ru.mail.jira.plugins.calendar.schedule.dayOfTheWeek")));
        if (schedule.equals(CronEditorBean.DAYS_OF_MONTH_SPEC_MODE) && monthDay == null)
            addError("calendar-schedule-interval-month-days", getText("issue.field.required", getText("ru.mail.jira.plugins.calendar.schedule.dayOfTheMonth")));
        if (schedule.equals(CronEditorBean.ADVANCED_MODE))
            if (StringUtils.isBlank(advanced))
                addError("calendar-schedule-interval-advanced", getText("issue.field.required", getText("ru.mail.jira.plugins.calendar.schedule.cronExpression")));
            else try {
                new CronExpressionParser(advanced);
            } catch (Exception e) {
                addError("calendar-schedule-interval-advanced", e.getMessage());
            }
    }

    @SuppressWarnings("UnusedDeclaration")
    public Integer getId() {
        return this.id;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setId(Integer id) {
        this.id = id;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getName() {
        return this.name;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setName(String name) {
        this.name = name;
    }

    @SuppressWarnings("UnusedDeclaration")
    public long getIssueId() {
        return this.issueId;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setIssueId(long issueId) {
        this.issueId = issueId;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getSchedule() {
        return this.schedule;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Integer getHours() {
        return this.hours;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setHours(Integer hours) {
        this.hours = hours;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Integer getMinutes() {
        return this.minutes;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String[] getWeekdays() {
        return this.weekdays;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setWeekdays(String[] weekdays) {
        this.weekdays = weekdays;
    }

    @SuppressWarnings("UnusedDeclaration")
    public Integer getMonthDay() {
        return this.monthDay;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setMonthDay(Integer monthDay) {
        this.monthDay = monthDay;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getAdvanced() {
        return this.advanced;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setAdvanced(String advanced) {
        this.advanced = advanced;
    }

    public Issue getIssueObject() {
        return issueManager.getIssueObject(issueId);
    }

    public Issue getIssue() {
        return issueManager.getIssueObject(issueId);
    }

    public Project getProjectObject() {
        return getIssueObject().getProjectObject();
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getProjectAvatarUrl() {
        return avatarService.getProjectAvatarAbsoluteURL(getProjectObject(), Avatar.Size.LARGE).toString();
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean weekdaysContains(String weekday) {
        return weekdays != null && Arrays.asList(weekdays).contains(weekday);
    }

    public Map<String, String[]> buildCronEditorBeanParams() throws ParseException {
        Map<String, String[]> result = new HashMap<String, String[]>();
        if (StringUtils.isNotBlank(schedule))
            result.put(String.format("%s.dailyWeeklyMonthly", Consts.SCHEDULE_PREFIX), new String[] {schedule});
        if (!schedule.equals(CronEditorBean.ADVANCED_MODE)) {
            result.put(String.format("%s.runOnceHours", Consts.SCHEDULE_PREFIX), new String[] {String.valueOf(calendarUtils.get12HourTime(hours))});
            result.put(String.format("%s.runOnceMins", Consts.SCHEDULE_PREFIX), new String[] {String.valueOf(minutes)});
            result.put(String.format("%s.runOnceMeridian", Consts.SCHEDULE_PREFIX), new String[] {calendarUtils.getMeridianIndicator(hours)});
        }
        if (schedule.equals(CronEditorBean.ADVANCED_MODE))
            result.put(String.format("%s.cronString", Consts.SCHEDULE_PREFIX), new String[] {advanced});
        if (schedule.equals(CronEditorBean.DAYS_OF_WEEK_SPEC_MODE))
            result.put(String.format("%s.weekday", Consts.SCHEDULE_PREFIX), weekdays);
        if (schedule.equals(CronEditorBean.DAYS_OF_MONTH_SPEC_MODE)) {
            result.put(String.format("%s.monthDay", Consts.SCHEDULE_PREFIX), new String[] {monthDay.toString()});
            result.put(String.format("%s.daysOfMonthOpt", Consts.SCHEDULE_PREFIX), new String[] {"dayOfMonth"});
            result.put(String.format("%s.day", Consts.SCHEDULE_PREFIX), new String[] {"1"});
            result.put(String.format("%s.week", Consts.SCHEDULE_PREFIX), new String[] {"1"});
        }
        result.put(String.format("%s.interval", Consts.SCHEDULE_PREFIX), new String[] {"0"});
        return result;
    }
}
