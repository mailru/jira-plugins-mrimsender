package ru.mail.jira.plugins.myteam.controller;

import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/issueCreation/settings")
@Produces(MediaType.APPLICATION_JSON)
public class IssueCreationSettingsController {

  private final IssueCreationSettingsService issueCreationSettingsService;

  public IssueCreationSettingsController(IssueCreationSettingsService issueCreationSettingsService) {
    this.issueCreationSettingsService = issueCreationSettingsService;
  }

  @GET
  @Path("/all")
  public List<IssueCreationSettingsDto> getAllChatsSettings() {
    return issueCreationSettingsService.getAllSettings();
  }

  @GET
  @Path("/chat/{id}")
  public IssueCreationSettingsDto getChatSettings(@PathParam("id") final int id) {
    return issueCreationSettingsService.getSettings(id);
  }

  @PUT
  @Path("/chat/{id}")
  public IssueCreationSettingsDto updateChatSettings(@PathParam("id") final int id, final IssueCreationSettingsDto settings) {
    return issueCreationSettingsService.updateSettings(id, settings);
  }

}
