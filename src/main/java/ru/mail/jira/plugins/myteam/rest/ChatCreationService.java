/* (C)2020 */
package ru.mail.jira.plugins.myteam.rest;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.model.MyteamChatMetaEntity;
import ru.mail.jira.plugins.myteam.model.MyteamChatRepository;
import ru.mail.jira.plugins.myteam.model.PluginData;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatMemberId;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatResponse;
import ru.mail.jira.plugins.myteam.rest.dto.ChatCreationDataDto;
import ru.mail.jira.plugins.myteam.rest.dto.ChatMemberDto;
import ru.mail.jira.plugins.myteam.rest.dto.ChatMetaDto;

@Controller
@Path(("/chats"))
public class ChatCreationService {
  private static final Logger log = Logger.getLogger(ChatCreationService.class);

  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final MyteamApiClient myteamApiClient;
  private final IssueManager issueManager;
  private final WatcherManager watcherManager;
  private final AvatarService avatarService;
  private final UserManager userManager;
  private final MyteamChatRepository myteamChatRepository;
  private final PluginData pluginData;
  private final UserData userData;

  @Autowired
  public ChatCreationService(
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport IssueManager issueManager,
      @ComponentImport WatcherManager watcherManager,
      @ComponentImport AvatarService avatarService,
      @ComponentImport UserManager userManager,
      MyteamApiClient myteamApiClient,
      MyteamChatRepository myteamChatRepository,
      PluginData pluginData,
      UserData userData) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.issueManager = issueManager;
    this.watcherManager = watcherManager;
    this.myteamApiClient = myteamApiClient;
    this.myteamChatRepository = myteamChatRepository;
    this.avatarService = avatarService;
    this.userManager = userManager;
    this.pluginData = pluginData;
    this.userData = userData;
  }

  @GET
  @Path("/chatData/{issueKey}")
  @Produces({MediaType.APPLICATION_JSON})
  public Response findChatData(@PathParam("issueKey") String issueKey) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();

    if (loggedInUser == null) return Response.status(Response.Status.UNAUTHORIZED).build();
    MyteamChatMetaEntity chatMeta = myteamChatRepository.findChatByIssueKey(issueKey);
    if (chatMeta == null) return Response.ok().build();

    // TODO create chat link somehow here !!!
    String chatLink = "http://" + chatMeta.getChatId();
    return Response.ok(new ChatMetaDto(chatLink)).build();
  }

  @GET
  @Path("/chatCreationData/{issueKey}")
  @Produces({MediaType.APPLICATION_JSON})
  public Response getChatCreationData(@PathParam("issueKey") String issueKey) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) return Response.status(Response.Status.UNAUTHORIZED).build();

    Issue currentIssue = issueManager.getIssueByKeyIgnoreCase(issueKey);
    if (currentIssue == null) return Response.ok().build();

    String chatName = String.join(": ", currentIssue.getKey(), currentIssue.getSummary());

    List<ChatMemberDto> availableChatUsers =
        Stream.concat(
                watcherManager.getWatchersUnsorted(currentIssue).stream(),
                Stream.of(loggedInUser, currentIssue.getAssignee(), currentIssue.getReporter()))
            .filter(Objects::nonNull)
            .distinct()
            // TODO filter chat creation disabled users here
            .sorted((u1, u2) -> u1.getDisplayName().compareToIgnoreCase(u2.getDisplayName()))
            .map(
                user ->
                    new ChatMemberDto(
                        user.getDisplayName(),
                        user.getId(),
                        avatarService
                            .getAvatarURL(loggedInUser, user, Avatar.Size.LARGE)
                            .toString()))
            .collect(Collectors.toList());

    return Response.ok(new ChatCreationDataDto(chatName, availableChatUsers)).build();
  }

  @POST
  @Path("/createChat/{issueKey}")
  @Produces({MediaType.APPLICATION_JSON})
  public Response createChat(
      @PathParam("issueKey") String issueKey,
      @FormParam("name") String chatName,
      @FormParam("members") List<Long> memberIds) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) return Response.status(Response.Status.UNAUTHORIZED).build();

    Issue currentIssue = issueManager.getIssueByKeyIgnoreCase(issueKey);
    if (currentIssue == null) return Response.ok().build();

    // TODO max chat members hardcoded here
    if (memberIds.size() >= 30) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    List<ChatMemberId> chatMembers =
        memberIds.stream()
            .map(memberId -> userManager.getUserById(memberId).orElse(null))
            .filter(Objects::nonNull)
            .map(user -> new ChatMemberId(userData.getMrimLogin(user)))
            .collect(Collectors.toList());

    boolean test = true;
    if (test == true) {
      myteamChatRepository.persistChat("some_random_chat_id", issueKey);
      return Response.ok(new ChatMetaDto("CHAT/LINK/HERE")).build();
    }

    try {
      HttpResponse<ChatResponse> chatMethodResponse =
          this.myteamApiClient.createChat(pluginData.getToken(), chatName, null, chatMembers, true);
      if (chatMethodResponse.getStatus() == 200
          && chatMethodResponse.getBody() != null
          && chatMethodResponse.getBody().getSn() != null) {
        String chatId = chatMethodResponse.getBody().getSn();
        myteamChatRepository.persistChat(chatId, issueKey);
        return Response.ok(new ChatMetaDto(chatId)).build();
      } else {
        log.error("Exception during chat creation chat sn not found");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    } catch (IOException | UnirestException e) {
      log.error("Exception during chat creation", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }
}
