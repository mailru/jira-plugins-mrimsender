package ru.mail.jira.plugins.calendar.service.applications;

import com.atlassian.greenhopper.api.customfield.ManagedCustomFieldsService;
import com.atlassian.greenhopper.api.issuetype.ManagedIssueTypesService;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;

@Scanned
public class JiraSoftwareHelperImpl implements JiraSoftwareHelper {
    private final ManagedIssueTypesService managedIssueTypesService;
    private final ManagedCustomFieldsService managedCustomFieldsService;

    public JiraSoftwareHelperImpl(
        @ComponentImport ManagedIssueTypesService managedIssueTypesService,
        @ComponentImport ManagedCustomFieldsService managedCustomFieldsService
    ) {
        this.managedIssueTypesService = managedIssueTypesService;
        this.managedCustomFieldsService = managedCustomFieldsService;
    }

    public boolean isAvailable() {
        return true;
    }

    @Override
    public CustomField getEpicLinkField() {
        return managedCustomFieldsService.getEpicLinkCustomField().get();
    }

    @Override
    public CustomField getRankField() {
        return managedCustomFieldsService.getRankCustomField().get();
    }

    @Override
    public IssueType getEpicIssueType() {
        return managedIssueTypesService.getEpicIssueType().get();
    }
}
