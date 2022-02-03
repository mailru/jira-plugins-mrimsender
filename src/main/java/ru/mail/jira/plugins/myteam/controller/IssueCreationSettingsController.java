/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller;

import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Path("/issueCreation/settings")
@Produces(MediaType.APPLICATION_JSON)
public class IssueCreationSettingsController {

  private final IssueCreationSettingsService issueCreationSettingsService;

  public IssueCreationSettingsController(
      IssueCreationSettingsService issueCreationSettingsService) {
    this.issueCreationSettingsService = issueCreationSettingsService;
  }

  @GET
  @Path("/all")
  public List<IssueCreationSettingsDto> getAllChatsSettings() {
    return issueCreationSettingsService.getAllSettings();
  }

  @GET
  @Path("/chat")
  public IssueCreationSettingsDto getChatSettings(@QueryParam("chatId") final String chatId) {
    return issueCreationSettingsService.getSettingsByChatId(chatId).orElse(null);
  }

  @PUT
  @Path("/chat")
  public IssueCreationSettingsDto updateChatSettings(
      @QueryParam("chatId") final int chatId, final IssueCreationSettingsDto settings) {
    return issueCreationSettingsService.updateSettings(chatId, settings);
  }
}
