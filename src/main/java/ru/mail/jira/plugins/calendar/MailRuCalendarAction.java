package ru.mail.jira.plugins.calendar;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.calendar.service.licence.LicenseService;
import ru.mail.jira.plugins.calendar.service.licence.LicenseStatus;

@Scanned
public class MailRuCalendarAction extends JiraWebActionSupport {
    private final ApplicationProperties applicationProperties;
    private final GlobalPermissionManager globalPermissionManager;
    private final LicenseService licenseService;

    public MailRuCalendarAction(
        @ComponentImport("com.atlassian.jira.config.properties.ApplicationProperties") ApplicationProperties applicationProperties,
        @ComponentImport GlobalPermissionManager globalPermissionManager,
        LicenseService licenseService
    ) {
        this.applicationProperties = applicationProperties;
        this.globalPermissionManager = globalPermissionManager;
        this.licenseService = licenseService;
    }

    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }

    @SuppressWarnings("unused")
    public boolean getIsAdmin() {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, getLoggedInUser());
    }

    @SuppressWarnings("unused")
    public LicenseStatus getLicenseStatus() {
        return licenseService.getLicenseStatus();
    }

    @SuppressWarnings("unused")
    public boolean isIso8601Used() {
        return applicationProperties.getOption(APKeys.JIRA_DATE_TIME_PICKER_USE_ISO8601);
    }
}
