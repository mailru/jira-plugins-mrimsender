package ru.mail.jira.plugins.calendar.service.licence;

public interface LicenseService {
    void checkLicense();

    LicenseStatus getLicenseStatus();
}
