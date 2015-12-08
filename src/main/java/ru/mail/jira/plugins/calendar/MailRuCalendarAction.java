package ru.mail.jira.plugins.calendar;

import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.calendar.rest.dto.DateField;
import ru.mail.jira.plugins.calendar.service.CalendarEventService;
import ru.mail.jira.plugins.calendar.service.UserDataService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MailRuCalendarAction extends JiraWebActionSupport {
    private final UserDataService userDataService;
    private final CustomFieldManager customFieldManager;

    private final I18nHelper i18nHelper;

    public MailRuCalendarAction(UserDataService userDataService,
                                CustomFieldManager customFieldManager,
                                I18nHelper i18nHelper) {
        this.userDataService = userDataService;
        this.customFieldManager = customFieldManager;
        this.i18nHelper = i18nHelper;
    }

    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }

    @SuppressWarnings("unused")
    public boolean isHideWeekends() {
        UserData userData = userDataService.getUserData(getLoggedInApplicationUser());
        return userData != null && userData.isHideWeekends();
    }

    //todo refactoring
    @SuppressWarnings("unused")
    public List<String> getColors() {
        return Arrays.asList("#5dab3e", "#d7ad43", "#3e6894", "#c9dad8", "#588e87", "#e18434",
                             "#83382A", "#D04A32", "#3C2B28", "#87A4C0", "#A89B95");
    }

    @SuppressWarnings("unused")
    public List<DateField> getDateFields() {
        List<DateField> dateFields = new ArrayList<DateField>();
        dateFields.add(DateField.of(CalendarEventService.CREATED_DATE_KEY, i18nHelper.getText("issue.field.created")));
        dateFields.add(DateField.of(CalendarEventService.UPDATED_DATE_KEY, i18nHelper.getText("issue.field.updated")));
        dateFields.add(DateField.of(CalendarEventService.RESOLVED_DATE_KEY, i18nHelper.getText("common.concepts.resolved")));
        dateFields.add(DateField.of(CalendarEventService.DUE_DATE_KEY, i18nHelper.getText("issue.field.duedate")));

        for (CustomField customField : customFieldManager.getCustomFieldObjects())
            if (customField.getCustomFieldType() instanceof com.atlassian.jira.issue.fields.DateField)
                dateFields.add(DateField.of(customField.getId(), customField.getName()));

        return dateFields;
    }
}
