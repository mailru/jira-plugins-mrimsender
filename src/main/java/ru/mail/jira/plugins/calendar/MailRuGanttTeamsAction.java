package ru.mail.jira.plugins.calendar;

import com.atlassian.jira.web.action.JiraWebActionSupport;
import ru.mail.jira.plugins.calendar.service.licence.LicenseService;
import ru.mail.jira.plugins.calendar.service.licence.LicenseStatus;

public class MailRuGanttTeamsAction extends JiraWebActionSupport {
    private final LicenseService licenseService;

    public MailRuGanttTeamsAction(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @SuppressWarnings("unused")
    public LicenseStatus getLicenseStatus() {
        return licenseService.getLicenseStatus();
    }
}
