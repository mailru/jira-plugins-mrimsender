/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller;

import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.SettingsTagAlreadyExistsException;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Controller
@Path("/issueCreation")
@Produces(MediaType.APPLICATION_JSON)
public class IssueCreationSettingsController {

  private final IssueCreationSettingsService issueCreationSettingsService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final PermissionHelper permissionHelper;

  public IssueCreationSettingsController(
      IssueCreationSettingsService issueCreationSettingsService,
      PermissionHelper permissionHelper,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
    this.issueCreationSettingsService = issueCreationSettingsService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.permissionHelper = permissionHelper;
  }

  @GET
  @Path("/settings/all")
  public List<IssueCreationSettingsDto> getAllChatsSettings() throws PermissionException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    permissionHelper.checkChatAdminPermissions(user);
    return issueCreationSettingsService.getAllSettings();
  }

  @GET
  @Path("/settings/{id}")
  public IssueCreationSettingsDto getChatSettingsById(@PathParam("id") final Integer id)
      throws PermissionException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    IssueCreationSettingsDto settings = issueCreationSettingsService.getSettingsById(id);
    permissionHelper.checkChatAdminPermissions(user, settings.getChatId());
    return issueCreationSettingsService.getSettingsById(id);
  }

  @GET
  @Path("/settings/chats/{id}")
  public List<IssueCreationSettingsDto> getChatSettings(@PathParam("id") final String id)
      throws PermissionException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    permissionHelper.checkChatAdminPermissions(user, id);
    return issueCreationSettingsService.getSettingsByChatId(id);
  }

  @GET
  @Path("/settings/projects/{projectKey}")
  public List<IssueCreationSettingsDto> getProjectChatSettings(
      @PathParam("projectKey") final String projectKey) throws PermissionException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    permissionHelper.checkProjectPermissions(user, projectKey);
    return issueCreationSettingsService.getSettingsByProjectKey(projectKey).stream()
        .peek(s -> s.setCanEdit(permissionHelper.isChatAdminOrJiraAdmin(s.getChatId(), user)))
        .collect(Collectors.toList());
  }

  @POST
  @RequiresXsrfCheck
  @Path("/settings")
  @Consumes(MediaType.APPLICATION_JSON)
  public Integer createChatSettings(final IssueCreationSettingsDto settings)
      throws SettingsTagAlreadyExistsException, PermissionException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    permissionHelper.checkChatAdminPermissions(user, settings.getChatId());
    IssueCreationSettingsDto originalSettings =
        issueCreationSettingsService.createSettings(settings);
    return originalSettings.getId();
  }

  @PUT
  @RequiresXsrfCheck
  @Path("/settings/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  public IssueCreationSettingsDto updateChatSettings(
      @PathParam("id") final int id, final IssueCreationSettingsDto settings)
      throws PermissionException, SettingsTagAlreadyExistsException {
    IssueCreationSettingsDto originalSettings = issueCreationSettingsService.getSettings(id);
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    permissionHelper.checkChatAdminPermissions(user, originalSettings.getChatId());
    return issueCreationSettingsService.updateSettings(id, settings);
  }

  @DELETE
  @RequiresXsrfCheck
  @Path("/settings/{id}")
  public void deleteChatSettings(@PathParam("id") final int id) throws PermissionException {
    IssueCreationSettingsDto settings = issueCreationSettingsService.getSettings(id);
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    permissionHelper.checkChatAdminPermissions(user, settings.getChatId());
    issueCreationSettingsService.deleteSettings(id);
  }
}
