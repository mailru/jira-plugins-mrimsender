/* (C)2024 */
package ru.mail.jira.plugins.myteam.controller.migrate;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsProjectKeyToIdMigrationResultDto;
import ru.mail.jira.plugins.myteam.service.migrate.IssueCreationSettingsProjectKeyToIdMigrationService;

@Path("/migrate/issueCreationSettings")
@Produces(MediaType.APPLICATION_JSON)
public class MigrationController {
  private final IssueCreationSettingsProjectKeyToIdMigrationService
      issueCreationSettingsProjectKeyToIdMigrationService;

  @Autowired
  public MigrationController(
      final IssueCreationSettingsProjectKeyToIdMigrationService
          issueCreationSettingsProjectKeyToIdMigrationService) {
    this.issueCreationSettingsProjectKeyToIdMigrationService =
        issueCreationSettingsProjectKeyToIdMigrationService;
  }

  @Path("/projKeyToId/all")
  @GET
  public IssueCreationSettingsProjectKeyToIdMigrationResultDto migrateAll() {
    return issueCreationSettingsProjectKeyToIdMigrationService.migrateAll();
  }
}
