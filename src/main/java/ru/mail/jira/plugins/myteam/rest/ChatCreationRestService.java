/* (C)2020 */
package ru.mail.jira.plugins.myteam.rest;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.config.LocaleManager;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.model.MyteamChatMetaEntity;
import ru.mail.jira.plugins.myteam.model.MyteamChatRepository;
import ru.mail.jira.plugins.myteam.model.PluginData;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMember;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMemberId;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.CreateChatResponse;
import ru.mail.jira.plugins.myteam.protocol.events.JiraIssueViewEvent;
import ru.mail.jira.plugins.myteam.protocol.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.rest.dto.ChatCreationDataDto;
import ru.mail.jira.plugins.myteam.rest.dto.ChatMemberDto;
import ru.mail.jira.plugins.myteam.rest.dto.ChatMetaDto;

@Controller
@Path("/chats")
public class ChatCreationRestService {
  private static final Logger log = LoggerFactory.getLogger(ChatCreationRestService.class);

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
  private final LocaleManager localeManager;
  private final ApplicationProperties applicationProperties;

  @Autowired
  public ChatCreationRestService(
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport IssueManager issueManager,
      @ComponentImport WatcherManager watcherManager,
      @ComponentImport AvatarService avatarService,
      @ComponentImport UserManager userManager,
      @ComponentImport UserSearchService userSearchService,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport LocaleManager localeManager,
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
    this.localeManager = localeManager;
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
    MyteamChatMetaEntity chatMeta = myteamChatRepository.findChatByIssueKey(issueKey);
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
      ChatMember chatMembersFromApi = myteamApiClient.getMembers(chatMeta.getChatId()).getBody();
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

      List<ChatMemberDto> chatMemberDtos =
          applicationUsers.stream()
              .map(
                  member -> {
                    String url = avatarService.getAvatarURL(loggedInUser, member).toString();
                    return new ChatMemberDto(member.getDisplayName(), member.getId(), url);
                  })
              .collect(Collectors.toList());

      HttpResponse<ChatInfoResponse> chatInfoResponse =
          myteamApiClient.getChatInfo(pluginData.getToken(), chatMeta.getChatId());
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
            .map(user -> new ChatMemberId(userData.getMrimLogin(user)))
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
    /* return Response.ok(
        ChatMetaDto.buildChatInfo(
            new GroupChatInfo(
                "title", "about", "rules", "http:/myteam/inite/link", false, false)))
    .build();*/
    try {
      Locale recipientLocale = localeManager.getLocaleFor(loggedInUser);
      HttpResponse<CreateChatResponse> createChatResponse =
          this.myteamApiClient.createChat(
              pluginData.getToken(),
              chatName,
              i18nResolver.getText(
                  recipientLocale,
                  "ru.mail.jira.plugins.myteam.createChat.about.text",
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

        HttpResponse<ChatInfoResponse> chatInfoResponse =
            myteamApiClient.getChatInfo(pluginData.getToken(), chatId);
        if (chatInfoResponse.getStatus() == 200 && chatInfoResponse.getBody() != null) {
          return ChatMetaDto.buildChatInfo(chatInfoResponse.getBody(), chatMemberDtos);
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
