package ru.mail.jira.plugins.calendar;

import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;

public class MailRuCalendarAction extends JiraWebActionSupport {
    private final GlobalPermissionManager globalPermissionManager;

    public MailRuCalendarAction(GlobalPermissionManager globalPermissionManager) {
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
}
