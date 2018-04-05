package ru.mail.jira.plugins.calendar.service.applications;

import com.atlassian.greenhopper.api.customfield.ManagedCustomFieldsService;
import com.atlassian.greenhopper.api.issuetype.ManagedIssueTypesService;
import com.atlassian.greenhopper.model.rapid.RapidView;
import com.atlassian.greenhopper.service.ServiceOutcome;
import com.atlassian.greenhopper.service.rapid.view.RapidViewService;
import com.atlassian.greenhopper.service.sprint.Sprint;
import com.atlassian.greenhopper.service.sprint.SprintService;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Scanned
public class JiraSoftwareHelperImpl implements JiraSoftwareHelper {
    private final ManagedIssueTypesService managedIssueTypesService;
    private final ManagedCustomFieldsService managedCustomFieldsService;
    private final SprintService sprintService;
    private final RapidViewService rapidViewService;

    public JiraSoftwareHelperImpl(
        @ComponentImport ManagedIssueTypesService managedIssueTypesService,
        @ComponentImport ManagedCustomFieldsService managedCustomFieldsService,
        @ComponentImport SprintService sprintService,
        @ComponentImport RapidViewService rapidViewService
    ) {
        this.managedIssueTypesService = managedIssueTypesService;
        this.managedCustomFieldsService = managedCustomFieldsService;
        this.sprintService = sprintService;
        this.rapidViewService = rapidViewService;
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
    public CustomField getSprintField() {
        return managedCustomFieldsService.getSprintCustomField().get();
    }

    @Override
    public IssueType getEpicIssueType() {
        return managedIssueTypesService.getEpicIssueType().get();
    }

    @Override
    public List<SprintDto> findSprints(ApplicationUser user, String query) {
        ServiceOutcome<Set<Sprint>> outcome = sprintService.findSprintsByName(user, query, 100, true);

        if (outcome.isValid()) {
            return outcome
                .get()
                .stream()
                .map(sprint -> {
                    SprintDto dto = new SprintDto();
                    dto.setId(sprint.getId());
                    dto.setName(sprint.getName());
                    ServiceOutcome<RapidView> rapidViewOutcome = rapidViewService.getRapidView(user, sprint.getRapidViewId());
                    if (rapidViewOutcome.isValid()) {
                        dto.setBoardName(rapidViewOutcome.get().getName());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
        } else {
            return ImmutableList.of();
        }
    }
}
