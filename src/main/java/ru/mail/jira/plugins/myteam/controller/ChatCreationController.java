/* (C)2020 */
package ru.mail.jira.plugins.myteam.controller;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.bot.events.JiraIssueViewEvent;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.controller.dto.ChatCreationDataDto;
import ru.mail.jira.plugins.myteam.controller.dto.ChatMemberDto;
import ru.mail.jira.plugins.myteam.controller.dto.ChatMetaDto;
import ru.mail.jira.plugins.myteam.db.model.MyteamChatMeta;
import ru.mail.jira.plugins.myteam.db.repository.MyteamChatRepository;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMember;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMemberId;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.CreateChatResponse;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Controller
@Slf4j
@Path("/chats")
public class ChatCreationController {
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
  private final UserSearchService userSearchService;
  private final I18nResolver i18nResolver;
  private final ApplicationProperties applicationProperties;

  @Autowired
  public ChatCreationController(
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport IssueManager issueManager,
      @ComponentImport WatcherManager watcherManager,
      @ComponentImport AvatarService avatarService,
      @ComponentImport UserManager userManager,
      @ComponentImport UserSearchService userSearchService,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport ApplicationProperties applicationProperties,
      MyteamApiClient myteamApiClient,
      MyteamChatRepository myteamChatRepository,
      PluginData pluginData,
      UserData userData,
      MyteamEventsListener myteamEventsListener) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.issueManager = issueManager;
    this.watcherManager = watcherManager;
    this.myteamApiClient = myteamApiClient;
    this.userSearchService = userSearchService;
    this.myteamChatRepository = myteamChatRepository;
    this.avatarService = avatarService;
    this.userManager = userManager;
    this.pluginData = pluginData;
    this.userData = userData;
    this.myteamEventsListener = myteamEventsListener;
    this.i18nResolver = i18nResolver;
    this.applicationProperties = applicationProperties;
  }

  @GET
  @Path("/chatData/{issueKey}")
  @Produces({MediaType.APPLICATION_JSON})
  public ChatMetaDto findChatData(@PathParam("issueKey") String issueKey) {

    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();

    if (loggedInUser == null) {
      throw new SecurityException();
    }
    MyteamChatMeta chatMeta = myteamChatRepository.findChatByIssueKey(issueKey);
    if (chatMeta == null) {
      return null;
    }
    /*
    //localhost test stubbing part
    return Response.ok(
            ChatMetaDto.buildChatInfo(
                new GroupChatInfo(
                    "title", "about", "rules", "http:/myteam/inite/link", false, false)))
        .build();*/
    try {
      List<ChatMemberDto> chatMemberDtos;
      ChatMember chatMembersFromApi = myteamApiClient.getMembers(chatMeta.getChatId()).getBody();
      if (chatMembersFromApi != null && chatMembersFromApi.members != null) {
        List<ApplicationUser> applicationUsers =
            chatMembersFromApi.members.stream()
                .map(
                    member ->
                        StreamSupport.stream(
                                userSearchService.findUsersByEmail(member.userId).spliterator(),
                                false)
                            .filter(ApplicationUser::isActive)
                            .findFirst()
                            .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        chatMemberDtos =
            applicationUsers.stream()
                .map(
                    member -> {
                      String url = avatarService.getAvatarURL(loggedInUser, member).toString();
                      return new ChatMemberDto(member.getDisplayName(), member.getId(), url);
                    })
                .collect(Collectors.toList());
      } else {
        chatMemberDtos = Collections.emptyList();
      }

      HttpResponse<ChatInfoResponse> chatInfoResponse =
          myteamApiClient.getChatInfo(chatMeta.getChatId());
      if (chatInfoResponse.getStatus() == 200 && chatInfoResponse.getBody() != null) {
        ChatInfoResponse chatInfo = chatInfoResponse.getBody();
        return ChatMetaDto.buildChatInfo(chatInfo, chatMemberDtos);
      } else {
        log.error(
            "getChatInfo method returns NOT OK status or empty body for chat with chatId = "
                + chatMeta.getChatId());
        throw new RuntimeException(
            "getChatInfo method returns NOT OK status or empty body for chat with chatId = "
                + chatMeta.getChatId());
      }
    } catch (UnirestException | MyteamServerErrorException e) {
      log.error("exception in getChatInfo method call for chatId = " + chatMeta.getChatId(), e);
      throw new RuntimeException(
          "exception in getChatInfo method call for chatId = " + chatMeta.getChatId(), e);
    }
  }

  @GET
  @Path("/chatCreationData/{issueKey}")
  @Produces({MediaType.APPLICATION_JSON})
  public ChatCreationDataDto getChatCreationData(@PathParam("issueKey") String issueKey) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    Issue currentIssue = issueManager.getIssueByKeyIgnoreCase(issueKey);
    if (currentIssue == null) {
      return null;
    }

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

    return new ChatCreationDataDto(chatName, availableChatUsers);
  }

  @POST
  @Path("/createChat/{issueKey}")
  @Produces({MediaType.APPLICATION_JSON})
  public ChatMetaDto createChat(
      @PathParam("issueKey") String issueKey,
      @FormParam("name") String chatName,
      @FormParam("memberIds") List<Long> memberIds) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    Issue currentIssue = issueManager.getIssueByKeyIgnoreCase(issueKey);
    // TODO max chat members hardcoded here
    if (currentIssue == null
        || pluginData.getChatCreationBannedProjectIds().contains(currentIssue.getProjectId())
        || memberIds.size() >= 30) {
      throw new IllegalArgumentException();
    }

    List<ChatMemberId> chatMembers =
        memberIds.stream()
            .map(memberId -> userManager.getUserById(memberId).orElse(null))
            .filter(Objects::nonNull)
            .filter(userData::isCreateChatsWithUserAllowed)
            .map(user -> new ChatMemberId(user.getEmailAddress()))
            .collect(Collectors.toList());

    List<ApplicationUser> applicationUsers =
        chatMembers.stream()
            .map(
                member ->
                    StreamSupport.stream(
                            userSearchService.findUsersByEmail(member.getSn()).spliterator(), false)
                        .filter(ApplicationUser::isActive)
                        .findFirst()
                        .orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    List<ChatMemberDto> chatMemberDtos =
        applicationUsers.stream()
            .map(
                member -> {
                  String url = avatarService.getAvatarURL(loggedInUser, member).toString();
                  return new ChatMemberDto(member.getDisplayName(), member.getId(), url);
                })
            .collect(Collectors.toList());
    // localhost tests stubbing
    /*return ChatMetaDto.buildChatInfo(
    new GroupChatInfo("title", "about", "rules", "http:/myteam/inite/link", false, false),
    chatMemberDtos);*/
    try {
      HttpResponse<CreateChatResponse> createChatResponse =
          this.myteamApiClient.createChat(
              pluginData.getToken(),
              chatName,
              i18nResolver.getText(
                  "ru.mail.jira.plugins.myteam.createChat.about.text",
                  StringUtils.EMPTY,
                  applicationProperties.getString(APKeys.JIRA_BASEURL) + "/browse/" + issueKey),
              chatMembers,
              false);
      if (createChatResponse.getStatus() == 200
          && createChatResponse.getBody() != null
          && createChatResponse.getBody().getSn() != null) {
        String chatId = createChatResponse.getBody().getSn();
        myteamChatRepository.persistChat(chatId, issueKey);
        myteamEventsListener.publishEvent(
            new JiraIssueViewEvent(chatId, issueKey, loggedInUser, true));

        HttpResponse<ChatInfoResponse> chatInfoResponse = myteamApiClient.getChatInfo(chatId);
        if (chatInfoResponse.getStatus() == 200 && chatInfoResponse.getBody() != null) {
          ChatMetaDto chatMetaDto =
              ChatMetaDto.buildChatInfo(chatInfoResponse.getBody(), chatMemberDtos);
          this.myteamApiClient.setAboutChat(
              pluginData.getToken(),
              chatId,
              i18nResolver.getText(
                  "ru.mail.jira.plugins.myteam.createChat.about.text",
                  chatMetaDto != null ? chatMetaDto.getLink() : StringUtils.EMPTY,
                  applicationProperties.getString(APKeys.JIRA_BASEURL) + "/browse/" + issueKey));
          return chatMetaDto;
        }
      }
      log.error("Exception during chat creation chat sn not found");

      throw new RuntimeException("Exception during chat creation chat sn not found");
    } catch (IOException | UnirestException | MyteamServerErrorException e) {
      log.error("Exception during chat creation", e);
      throw new RuntimeException("Exception during chat creation", e);
    }
  }

  @GET
  @Path("/chatCreationData/users")
  @Produces({MediaType.APPLICATION_JSON})
  public List<ChatMemberDto> getAvailableChatMembers(@QueryParam("searchText") String searchText) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }
    return userSearchService
        .findUsersAllowEmptyQuery(new JiraServiceContextImpl(loggedInUser), searchText).stream()
        .map(
            user ->
                new ChatMemberDto(
                    user.getDisplayName(),
                    user.getId(),
                    avatarService.getAvatarURL(loggedInUser, user, Avatar.Size.LARGE).toString()))
        .collect(Collectors.toList());
  }
}
