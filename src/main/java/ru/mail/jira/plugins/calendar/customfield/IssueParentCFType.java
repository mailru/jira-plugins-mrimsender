package ru.mail.jira.plugins.calendar.customfield;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.impl.AbstractSingleFieldType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.rest.FieldJsonRepresentation;
import com.atlassian.jira.issue.fields.rest.FieldTypeInfo;
import com.atlassian.jira.issue.fields.rest.FieldTypeInfoContext;
import com.atlassian.jira.issue.fields.rest.RestAwareCustomFieldType;
import com.atlassian.jira.issue.fields.rest.json.JsonData;
import com.atlassian.jira.issue.fields.rest.json.JsonType;
import com.atlassian.jira.issue.fields.rest.json.JsonTypeBuilder;
import com.atlassian.jira.web.bean.BulkEditBean;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class IssueParentCFType extends AbstractSingleFieldType<String> implements SortableCustomField<String>, RestAwareCustomFieldType {
    private final ApplicationProperties applicationProperties;

    public IssueParentCFType(@ComponentImport final CustomFieldValuePersister customFieldValuePersister,
                             @ComponentImport final ApplicationProperties applicationProperties,
                             @ComponentImport final GenericConfigManager genericConfigManager) {
        super(customFieldValuePersister, genericConfigManager);
        this.applicationProperties = applicationProperties;
    }

    public String getStringFromSingularObject(final String value) {
        return value;
    }

    public String getSingularObjectFromString(final String value) throws FieldValidationException {
        return value;
    }

    @Override
    public void updateValue(final CustomField customField, final Issue issue, final String value) {
        if (value != null) {
            super.updateValue(customField, issue, value);
        }
    }

    @Nonnull
    @Override
    public Map<String, Object> getVelocityParameters(final Issue issue, final CustomField field, final FieldLayoutItem fieldLayoutItem) {
        final Map<String, Object> params = new HashMap<>();
        params.putAll(super.getVelocityParameters(issue, field, fieldLayoutItem));
        return params;
    }

    // Read only - not editable
    @Override
    public String availableForBulkEdit(final BulkEditBean bulkEditBean) {
        return "bulk.edit.unavailable";
    }

    @Override
    public String getChangelogValue(final CustomField field, final String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            return value;
        }
    }

    @Nonnull
    @Override
    protected PersistenceFieldType getDatabaseType() {
        return PersistenceFieldType.TYPE_LIMITED_TEXT;
    }

    @Nullable
    @Override
    protected Object getDbValueFromObject(String value) {
        return value;
    }

    @Nullable
    @Override
    protected String getObjectFromDbValue(@Nonnull Object value) throws FieldValidationException {
        return (String) value;
    }

    @Override
    public int compare(@Nonnull String v1, @Nonnull String v2, FieldConfig fieldConfig) {
        return v1.compareTo(v2);
    }

    @Override
    public FieldTypeInfo getFieldTypeInfo(FieldTypeInfoContext fieldTypeInfoContext) {
        return new FieldTypeInfo(null, null);
    }

    @Override
    public JsonType getJsonSchema(CustomField customField) {
        return JsonTypeBuilder.custom(JsonType.STRING_TYPE, getKey(), customField.getIdAsLong());
    }

    @Override
    public FieldJsonRepresentation getJsonFromIssue(CustomField field, Issue issue, boolean renderedVersionRequested, @Nullable FieldLayoutItem fieldLayoutItem) {
        String value = getValueFromIssue(field, issue);
        return new FieldJsonRepresentation(new JsonData(value));
    }
}