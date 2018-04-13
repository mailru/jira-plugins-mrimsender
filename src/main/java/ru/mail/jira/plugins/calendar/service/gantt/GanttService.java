package ru.mail.jira.plugins.calendar.service.gantt;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.user.ApplicationUser;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.*;
import ru.mail.jira.plugins.calendar.rest.dto.plan.GanttPlanForm;

import java.util.List;

@Transactional
public interface GanttService {
    GanttDto getGantt(ApplicationUser user, int calendarId, String startDate, String endDate, GanttParams params) throws Exception;
    GanttDto getGantt(ApplicationUser user, int calendarId, GanttParams params) throws Exception;

    GanttLinkDto createLink(ApplicationUser user, int calendarId, GanttLinkForm form) throws GetException;
    void deleteLink(ApplicationUser user, int calendarId, int linkId) throws GetException;

    boolean hasLinks(String issueKey);
    List<GanttLinkDto> getLinks(String issueKey);

    List<GanttTaskDto> updateDates(ApplicationUser user, int calendarId, String issueKey, String startDate, String endDate) throws Exception;

    void applyPlan(ApplicationUser loggedInUser, int calendarId, GanttPlanForm form) throws Exception;

    GanttTaskDto setEstimate(ApplicationUser loggedInUser, int calendarId, String issueKey, GanttEstimateForm form) throws Exception;
}
