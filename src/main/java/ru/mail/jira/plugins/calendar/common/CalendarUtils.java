package ru.mail.jira.plugins.calendar.common;

import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.JiraUtils;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Locale;

@Component
public class CalendarUtils {
    private final DateTimeFormatter dateTimeFormatter;

    private Locale locale;

    @Autowired
    public CalendarUtils(
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
            @ComponentImport DateTimeFormatter dateTimeFormatter
    ) {
        this.locale = jiraAuthenticationContext.getI18nHelper().getLocale();
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public String getFormattedDateTime(ApplicationUser user, Date date) {
        return (date == null) ? null : dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.COMPLETE).format(date);
    }

    public int get12HourTime(int hours) {
        return hours == 12 ? hours : hours % 12;
    }

    public int get24HourTime(int hours, String meridianIndicator) {
        return JiraUtils.get24HourTime(meridianIndicator, hours);
    }

    public String getMeridianIndicator(int hours) {
        return hours >=12 ? "PM" : "AM";
    }
}
