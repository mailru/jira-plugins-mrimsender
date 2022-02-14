/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller;

import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Controller
@Path("/issueCreation/settings")
@Produces(MediaType.APPLICATION_JSON)
public class IssueCreationSettingsController {

  private final IssueCreationSettingsService issueCreationSettingsService;
  private final UserChatService userChatService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final GlobalPermissionManager globalPermissionManager;

  @ResponseStatus(value = HttpStatus.FORBIDDEN)
  @ExceptionHandler(PermissionException.class)
  public String noRightsHandleException(PermissionException e) {
    return e.getLocalizedMessage();
  }

  public IssueCreationSettingsController(
      IssueCreationSettingsService issueCreationSettingsService,
      UserChatService userChatService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport GlobalPermissionManager globalPermissionManager) {
    this.issueCreationSettingsService = issueCreationSettingsService;
    this.userChatService = userChatService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.globalPermissionManager = globalPermissionManager;
  }

  @GET
  @Path("/all")
  public List<IssueCreationSettingsDto> getAllChatsSettings() {
    return issueCreationSettingsService.getAllSettings();
  }

  @GET
  @Path("/chats/{id}")
  public IssueCreationSettingsDto getChatSettings(@PathParam("id") final String chatId)
      throws PermissionException {
    checkPermissions(chatId);
    return issueCreationSettingsService.getSettingsByChatId(chatId).orElse(null);
  }

  @PUT
  @Path("/{id}")
  @Consumes(MediaType.APPLICATION_JSON)
  public IssueCreationSettingsDto updateChatSettings(
      @PathParam("id") final int id, final IssueCreationSettingsDto settings)
      throws PermissionException {
    IssueCreationSettingsDto originalSettings = issueCreationSettingsService.getSettings(id);
    checkPermissions(originalSettings.getChatId());
    return issueCreationSettingsService.updateSettings(id, settings);
  }

  private void checkPermissions(String chatId) throws PermissionException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

    if (isJiraAdmin(user) || userChatService.isChatAdmin(chatId, user.getEmailAddress())) {
      return;
    }
    throw new PermissionException();
  }

  private boolean isJiraAdmin(ApplicationUser user) {
    return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
  }
}
