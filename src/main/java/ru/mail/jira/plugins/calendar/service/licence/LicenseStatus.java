package ru.mail.jira.plugins.calendar.service.licence;

import lombok.Getter;

@Getter
public class LicenseStatus {
    private final boolean valid;
    private final String error;

    private LicenseStatus(boolean valid, String error) {
        this.valid = valid;
        this.error = error;
    }

    public static LicenseStatus ok() {
        return new LicenseStatus(true, null);
    }

    public static LicenseStatus invalid(String error) {
        return new LicenseStatus(false, error);
    }
}
