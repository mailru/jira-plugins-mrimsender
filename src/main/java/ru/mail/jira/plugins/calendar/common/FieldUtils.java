package ru.mail.jira.plugins.calendar.common;

import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;

public class FieldUtils {
    private FieldUtils() {}

    public static String getSearcherKey(CustomField customField) {
        CustomFieldSearcher customFieldSearcher = customField.getCustomFieldSearcher();
        if (customFieldSearcher != null) {
            return customFieldSearcher.getDescriptor().getCompleteKey();
        }
        return null;
    }

    public static boolean isDateField(CustomField customField) {
        CustomFieldType customFieldType = customField.getCustomFieldType();
        return customFieldType instanceof com.atlassian.jira.issue.fields.DateField || isScriptRunnerField(customField);
    }

    public static boolean isScriptRunnerField(CustomField customField) {
        return Consts.SR_FIELD_KEY.equals(customField.getCustomFieldType().getKey()) &&
            Consts.SR_DATE_SEARCHER_KEY.equals(FieldUtils.getSearcherKey(customField));
    }
}
