package ru.mail.jira.plugins.calendar.configuration;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

import java.util.List;

@Scanned
public class MailRuCalendarWorkingDaysConfigurationAction extends JiraWebActionSupport {
    private final WorkingDaysService workingDaysService;

    public MailRuCalendarWorkingDaysConfigurationAction(WorkingDaysService workingDaysService) {
        this.workingDaysService = workingDaysService;
    }

    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }

    public List<Integer> getWorkingDays() {
        return workingDaysService.getWorkingDays();
    }

    public WorkingTimeDto getWorkingTime() {
        return workingDaysService.getWorkingTime();
    }
}
