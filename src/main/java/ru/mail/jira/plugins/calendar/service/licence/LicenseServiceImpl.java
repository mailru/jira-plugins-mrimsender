package ru.mail.jira.plugins.calendar.service.licence;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.upm.api.license.PluginLicenseManager;
import com.atlassian.upm.api.license.entity.LicenseError;
import com.atlassian.upm.api.license.entity.PluginLicense;
import com.atlassian.upm.api.util.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Scanned
public class LicenseServiceImpl implements LicenseService {
    private final Logger logger = LoggerFactory.getLogger(LicenseServiceImpl.class);
    private final PluginLicenseManager pluginLicenseManager;
    private final I18nResolver i18nResolver;

    public LicenseServiceImpl(
        @ComponentImport PluginLicenseManager pluginLicenseManager,
        @ComponentImport I18nResolver i18nResolver
    ) {
        this.pluginLicenseManager = pluginLicenseManager;
        this.i18nResolver = i18nResolver;
    }

    @Override
    public void checkLicense() {
        LicenseStatus licenseStatus = getLicenseStatus();

        if (!licenseStatus.isValid()) {
            throw new SecurityException(licenseStatus.getError());
        }

        logger.debug("license ok");
    }

    @Override
    public LicenseStatus getLicenseStatus() {
        Option<PluginLicense> licenseOption = pluginLicenseManager.getLicense();
        if (licenseOption.isDefined()) {
            PluginLicense license = licenseOption.get();
            if (license.isValid()) {
                return LicenseStatus.ok();
            } else {
                Option<LicenseError> licenseErrorOption = license.getError();
                if (licenseErrorOption.isDefined()) {
                    LicenseError licenseError = licenseErrorOption.get();
                    return LicenseStatus.invalid(i18nResolver.getText("ru.mail.jira.plugins.license.error." + licenseError.name()));
                } else {
                    return LicenseStatus.invalid(i18nResolver.getText("ru.mail.jira.plugins.license.error.INVALID"));
                }
            }
        } else {
            return LicenseStatus.invalid(i18nResolver.getText("ru.mail.jira.plugins.license.error.REQUIRED"));
        }
    }
}
