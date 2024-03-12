package ru.mail.jira.plugins.myteam.controller.migrate;

import org.springframework.beans.factory.annotation.Autowired;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsProjectKeyToIdMigrationResultDto;
import ru.mail.jira.plugins.myteam.service.migrate.IssueCreationSettingsProjectKeyToIdMigrationService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/migrate")
@Produces(MediaType.APPLICATION_JSON)
public class MigrationController {
    private final IssueCreationSettingsProjectKeyToIdMigrationService issueCreationSettingsProjectKeyToIdMigrationService;

    @Autowired
    public MigrationController(final IssueCreationSettingsProjectKeyToIdMigrationService issueCreationSettingsProjectKeyToIdMigrationService) {
        this.issueCreationSettingsProjectKeyToIdMigrationService = issueCreationSettingsProjectKeyToIdMigrationService;
    }

    @Path("/issueCreationSettings/all")
    @GET
    public IssueCreationSettingsProjectKeyToIdMigrationResultDto migrateAll() {
        return issueCreationSettingsProjectKeyToIdMigrationService.migrateAll();
    }

    @Path("/issueCreationSettings/{projectKey}")
    @GET
    public IssueCreationSettingsProjectKeyToIdMigrationResultDto migrateAll(@PathParam("projectKey") String projectKey) {
        return issueCreationSettingsProjectKeyToIdMigrationService.migrateByProjectKey(projectKey);
    }
}
