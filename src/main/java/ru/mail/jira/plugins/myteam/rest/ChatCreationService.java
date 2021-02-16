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
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.model.MyteamChatMetaEntity;
import ru.mail.jira.plugins.myteam.model.MyteamChatRepository;
import ru.mail.jira.plugins.myteam.model.PluginData;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMemberId;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.CreateChatResponse;
import ru.mail.jira.plugins.myteam.protocol.events.JiraIssueViewEvent;
import ru.mail.jira.plugins.myteam.protocol.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.rest.dto.ChatCreationDataDto;
import ru.mail.jira.plugins.myteam.rest.dto.ChatMemberDto;
import ru.mail.jira.plugins.myteam.rest.dto.ChatMetaDto;

@Controller
@Path("/chats")
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
  private final MyteamEventsListener myteamEventsListener;

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
      UserData userData,
      MyteamEventsListener myteamEventsListener) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.issueManager = issueManager;
    this.watcherManager = watcherManager;
    this.myteamApiClient = myteamApiClient;
    this.myteamChatRepository = myteamChatRepository;
    this.avatarService = avatarService;
    this.userManager = userManager;
    this.pluginData = pluginData;
    this.userData = userData;
    this.myteamEventsListener = myteamEventsListener;
  }

  @GET
  @Path("/chatData/{issueKey}")
  @Produces({MediaType.APPLICATION_JSON})
  public Response findChatData(@PathParam("issueKey") String issueKey) {

    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();

    if (loggedInUser == null) return Response.status(Response.Status.UNAUTHORIZED).build();
    MyteamChatMetaEntity chatMeta = myteamChatRepository.findChatByIssueKey(issueKey);
    if (chatMeta == null) return Response.ok().build();
    /*
    localhost test stubbing part
    return Response.ok(
            ChatMetaDto.buildChatInfo(
                new GroupChatInfo(
                    "title", "about", "rules", "http:/myteam/inite/link", false, false)))
        .build();*/
    try {
      HttpResponse<ChatInfoResponse> chatInfoResponse =
          myteamApiClient.getChatInfo(pluginData.getToken(), chatMeta.getChatId());
      if (chatInfoResponse.getStatus() == 200 && chatInfoResponse.getBody() != null) {
        ChatInfoResponse chatInfo = chatInfoResponse.getBody();
        return Response.ok(ChatMetaDto.buildChatInfo(chatInfo)).build();
      } else {
        log.error(
            "getChatInfo method returns NOT OK status or empty body for chat with chatId = "
                + chatMeta.getChatId());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
      }
    } catch (UnirestException | MyteamServerErrorException e) {
      log.error("exception in getChatInfo method call for chatId = " + chatMeta.getChatId(), e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
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
            .filter(userData::isCreateChatsWithUserAllowed)
            .sorted((u1, u2) -> u1.getDisplayName().compareToIgnoreCase(u2.getDisplayName()))
            // TODO max chat members hardcoded here
            .limit(30)
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
      @FormParam("memberIds") List<Long> memberIds) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) return Response.status(Response.Status.UNAUTHORIZED).build();

    Issue currentIssue = issueManager.getIssueByKeyIgnoreCase(issueKey);
    // TODO max chat members hardcoded here
    if (currentIssue == null
        || pluginData.getChatCreationBannedProjectIds().contains(currentIssue.getProjectId())
        || memberIds.size() >= 30) return Response.status(Response.Status.BAD_REQUEST).build();

    List<ChatMemberId> chatMembers =
        memberIds.stream()
            .map(memberId -> userManager.getUserById(memberId).orElse(null))
            .filter(Objects::nonNull)
            .filter(userData::isCreateChatsWithUserAllowed)
            .map(user -> new ChatMemberId(userData.getMrimLogin(user)))
            .collect(Collectors.toList());

    // localhost tests stubbing
    /* return Response.ok(
        ChatMetaDto.buildChatInfo(
            new GroupChatInfo(
                "title", "about", "rules", "http:/myteam/inite/link", false, false)))
    .build();*/
    try {
      HttpResponse<CreateChatResponse> createChatResponse =
          this.myteamApiClient.createChat(
              pluginData.getToken(), chatName, null, chatMembers, false);
      if (createChatResponse.getStatus() == 200
          && createChatResponse.getBody() != null
          && createChatResponse.getBody().getSn() != null) {
        String chatId = createChatResponse.getBody().getSn();
        myteamChatRepository.persistChat(chatId, issueKey);
        myteamEventsListener.publishEvent(
            new JiraIssueViewEvent(chatId, issueKey, loggedInUser, true));

        HttpResponse<ChatInfoResponse> chatInfoResponse =
            myteamApiClient.getChatInfo(pluginData.getToken(), chatId);
        if (chatInfoResponse.getStatus() == 200 && chatInfoResponse.getBody() != null) {
          return Response.ok(ChatMetaDto.buildChatInfo(chatInfoResponse.getBody())).build();
        }
      }
      log.error("Exception during chat creation chat sn not found");
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } catch (IOException | UnirestException | MyteamServerErrorException e) {
      log.error("Exception during chat creation", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /*
  TODO this REST is needed for user-picker search requests in future
  @GET
   @Path("/chatCreationData/{issueKey}/availableMembers/{input:.*}")
   @Produces({MediaType.APPLICATION_JSON})
   public Response getAvailableChatMembers(
       @PathParam("issueKey") String issueKey, @PathParam("input") String input) {
     ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
     if (loggedInUser == null) return Response.status(Response.Status.UNAUTHORIZED).build();

     Issue currentIssue = issueManager.getIssueByKeyIgnoreCase(issueKey);
     if (currentIssue == null) return Response.ok().build();

     return Response.ok(
             userSearchService
                 .findUsersAllowEmptyQuery(new JiraServiceContextImpl(loggedInUser), input).stream()
                 .map(
                     user ->
                         new ChatMemberDto(
                             user.getDisplayName(),
                             user.getId(),
                             avatarService
                                 .getAvatarURL(loggedInUser, user, Avatar.Size.LARGE)
                                 .toString()))
                 .collect(Collectors.toList()))
         .build();
   }*/
}
