package ru.mail.jira.plugins.calendar.customfield;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.converters.DoubleConverter;
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
import com.atlassian.jira.util.velocity.NumberTool;
import com.atlassian.jira.web.bean.BulkEditBean;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class IssueDurationCFType extends AbstractSingleFieldType<Double> implements SortableCustomField<Double>, RestAwareCustomFieldType {
    private final ApplicationProperties applicationProperties;
    private final DoubleConverter doubleConverter;

    public IssueDurationCFType(@ComponentImport final CustomFieldValuePersister customFieldValuePersister,
                               @ComponentImport final ApplicationProperties applicationProperties,
                               @ComponentImport final GenericConfigManager genericConfigManager,
                               @ComponentImport final DoubleConverter doubleConverter) {
        super(customFieldValuePersister, genericConfigManager);
        this.applicationProperties = applicationProperties;
        this.doubleConverter = doubleConverter;
    }

    public String getStringFromSingularObject(final Double customFieldObject) {
        return doubleConverter.getString(customFieldObject);
    }

    public Double getSingularObjectFromString(final String string) throws FieldValidationException {
        return doubleConverter.getDouble(string);
    }

    @Override
    public void updateValue(final CustomField customField, final Issue issue, final Double value) {
        if (value != null) {
            super.updateValue(customField, issue, value);
        }
    }

    @Nonnull
    @Override
    public Map<String, Object> getVelocityParameters(final Issue issue, final CustomField field, final FieldLayoutItem fieldLayoutItem) {
        final Map<String, Object> params = new HashMap<>();
        params.putAll(super.getVelocityParameters(issue, field, fieldLayoutItem));
        params.put("numberTool", new NumberTool(getI18nBean().getLocale()));
        return params;
    }

    // Read only - not editable
    @Override
    public String availableForBulkEdit(final BulkEditBean bulkEditBean) {
        return "bulk.edit.unavailable";
    }

    @Override
    public String getChangelogValue(final CustomField field, final Double value) {
        if (value == null) {
            return "0";
        } else {
            return doubleConverter.getStringForChangelog(value);
        }
    }

    @Nonnull
    @Override
    protected PersistenceFieldType getDatabaseType() {
        return PersistenceFieldType.TYPE_DECIMAL;
    }

    @Nullable
    @Override
    protected Object getDbValueFromObject(Double value) {
        return value;
    }

    @Nullable
    @Override
    protected Double getObjectFromDbValue(@Nonnull Object value) throws FieldValidationException {
        return ((Double) value);
    }

    @Override
    public int compare(@Nonnull Double v1, @Nonnull Double v2, FieldConfig fieldConfig) {
        return v1.compareTo(v2);
    }

    @Override
    public FieldTypeInfo getFieldTypeInfo(FieldTypeInfoContext fieldTypeInfoContext) {
        return new FieldTypeInfo(null, null);
    }

    @Override
    public JsonType getJsonSchema(CustomField customField) {
        return JsonTypeBuilder.custom(JsonType.NUMBER_TYPE, getKey(), customField.getIdAsLong());
    }

    @Override
    public FieldJsonRepresentation getJsonFromIssue(CustomField field, Issue issue, boolean renderedVersionRequested, @Nullable FieldLayoutItem fieldLayoutItem) {
        Double number = getValueFromIssue(field, issue);
        return new FieldJsonRepresentation(new JsonData(number));
    }
}