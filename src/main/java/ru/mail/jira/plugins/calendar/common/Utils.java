package ru.mail.jira.plugins.calendar.common;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

@Scanned
public class Utils {
    private final GlobalPermissionManager globalPermissionManager;

    public Utils(@ComponentImport GlobalPermissionManager globalPermissionManager) {
        this.globalPermissionManager = globalPermissionManager;
    }

    public boolean isJiraAdmin(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }
}
