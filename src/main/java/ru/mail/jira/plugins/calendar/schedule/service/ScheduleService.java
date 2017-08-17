package ru.mail.jira.plugins.calendar.schedule.service;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.scheduler.config.JobId;
import ru.mail.jira.plugins.calendar.schedule.model.Schedule;

import java.util.Map;

public interface ScheduleService {
    Schedule getSchedule(int id);
    Map<String, String[]> getScheduleParams(int id) throws Exception;

    boolean hasPermissionToEditAndDelete(Schedule schedule, ApplicationUser user);

    void createSchedule(long issueId, String name, String mode, Map<String, String[]> scheduleParams);
    void updateSchedule(int id, String name, String mode, Map<String, String[]> scheduleParams);
    void deleteSchedule(int id);

    void cloneIssue(int scheduleId);
    JobId getJobId(int scheduleId);
}
