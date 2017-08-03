package ru.mail.jira.plugins.calendar.service.licence;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;

@Scanned
public class LicenseServiceStub implements LicenseService {
    @Override
    public void checkLicense() {

    }

    @Override
    public LicenseStatus getLicenseStatus() {
        return LicenseStatus.ok();
    }
}
