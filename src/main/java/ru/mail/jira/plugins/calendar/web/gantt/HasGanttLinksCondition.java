package ru.mail.jira.plugins.calendar.web.gantt;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.conditions.AbstractIssueWebCondition;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import ru.mail.jira.plugins.calendar.service.gantt.GanttService;

@Scanned
public class HasGanttLinksCondition extends AbstractIssueWebCondition {
    private final GanttService ganttService;

    public HasGanttLinksCondition(GanttService ganttService) {
        this.ganttService = ganttService;
    }

    @Override
    public boolean shouldDisplay(ApplicationUser applicationUser, Issue issue, JiraHelper jiraHelper) {
        return ganttService.hasLinks(issue.getKey());
    }
}
