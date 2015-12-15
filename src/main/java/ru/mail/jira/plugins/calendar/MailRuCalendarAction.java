package ru.mail.jira.plugins.calendar;

import com.atlassian.jira.web.action.JiraWebActionSupport;

public class MailRuCalendarAction extends JiraWebActionSupport {

    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }
}
