/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import org.codehaus.jackson.map.ObjectMapper;
import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.dto.TestDto;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Path("/issueCreation/settings")
// @Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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
  @Path("")
  public IssueCreationSettingsDto getChatSettings(@QueryParam("chatId") final String chatId) {
    return issueCreationSettingsService.getSettingsByChatId(chatId).orElse(null);
  }

  //  @PUT
  //  @Path("")
  //  //  @Consumes(MediaType.TEXT_PLAIN)
  //  @Consumes(MediaType.APPLICATION_JSON)
  //  public IssueCreationSettingsDto updateChatSettings(
  //      //      @QueryParam("chatId") final int chatId,
  //      final IssueCreationSettingsDto settings) {
  //    return issueCreationSettingsService.updateSettings(settings.getId(), settings);
  //  }

  @POST
  @Path("")
  //  @Consumes(MediaType.APPLICATION_JSON)
  public IssueCreationSettingsDto createChatSettings(IssueCreationSettingsDto settings) {
    return settings;
    //    return issueCreationSettingsService.updateSettings(settings.getId(), settings);
  }

  @PUT
  @Path("/test")
  @Consumes(MediaType.TEXT_PLAIN)
  public TestDto testupdateChatSettings(String test) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    TestDto res = mapper.readValue(test, TestDto.class);
    return res;
  }

  @PUT
  @Path("/test1")
  //    @Consumes(MediaType.APPLICATION_JSON)
  //  @Produces(MediaType.APPLICATION_JSON)
  public HashMap<String, String> testupdateChatSettings(TestDto test) {

    return new HashMap<>(ImmutableMap.of("field", test.getField()));
  }
}
