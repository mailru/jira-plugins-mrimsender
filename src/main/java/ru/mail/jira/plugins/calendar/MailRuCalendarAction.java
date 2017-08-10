package ru.mail.jira.plugins.calendar;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.calendar.service.licence.LicenseService;
import ru.mail.jira.plugins.calendar.service.licence.LicenseStatus;

@Scanned
public class MailRuCalendarAction extends JiraWebActionSupport {
    private final LicenseService licenseService;
    private final GlobalPermissionManager globalPermissionManager;

    public MailRuCalendarAction(
        LicenseService licenseService,
        @ComponentImport GlobalPermissionManager globalPermissionManager
    ) {
        this.licenseService = licenseService;
        this.globalPermissionManager = globalPermissionManager;
    }

    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }

    @SuppressWarnings("unused")
    public boolean getIsAdmin() {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, getLoggedInApplicationUser());
    }

    @SuppressWarnings("unused")
    public LicenseStatus getLicenseStatus() {
        return licenseService.getLicenseStatus();
    }
}
