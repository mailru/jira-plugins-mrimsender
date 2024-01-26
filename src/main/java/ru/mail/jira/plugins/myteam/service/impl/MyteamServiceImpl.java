/* (C)2020 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.JiraIssueViewEvent;
import ru.mail.jira.plugins.myteam.bot.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.LinkIssueWithChatException;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.controller.dto.ChatMemberDto;
import ru.mail.jira.plugins.myteam.controller.dto.ChatMetaDto;
import ru.mail.jira.plugins.myteam.db.model.MyteamChatMeta;
import ru.mail.jira.plugins.myteam.db.repository.MyteamChatRepository;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMemberId;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.CreateChatResponse;
import ru.mail.jira.plugins.myteam.protocol.MyteamService;
import ru.mail.jira.plugins.myteam.service.PluginData;
import ru.mail.jira.plugins.myteam.service.model.MyteamChatMetaDto;

@Service
@Slf4j
public class MyteamServiceImpl implements MyteamService {
  private final GroupManager groupManager;
  private final UserData userData;
  private final MyteamEventsListener myteamEventsListener;

  private final MyteamApiClient myteamApiClient;
  private final PluginData pluginData;
  private final I18nResolver i18nResolver;
  private final MyteamChatRepository myteamChatRepository;

  private final ApplicationProperties applicationProperties;

  private final UserManager userManager;

  private final UserSearchService userSearchService;

  private final AvatarService avatarService;

  private final IssueManager issueManager;

  @Autowired
  public MyteamServiceImpl(
      @ComponentImport GroupManager groupManager,
      MyteamEventsListener myteamEventsListener,
      UserData userData,
      MyteamApiClient myteamApiClient,
      PluginData pluginData,
      @ComponentImport I18nResolver i18nResolver,
      MyteamChatRepository myteamChatRepository,
      @ComponentImport ApplicationProperties applicationProperties,
      @ComponentImport UserManager userManager,
      @ComponentImport UserSearchService userSearchService,
      @ComponentImport AvatarService avatarService,
      @ComponentImport IssueManager issueManager) {
    this.groupManager = groupManager;
    this.userData = userData;
    this.myteamEventsListener = myteamEventsListener;
    this.issueManager = issueManager;
    this.myteamApiClient = myteamApiClient;
    this.pluginData = pluginData;
    this.i18nResolver = i18nResolver;
    this.myteamChatRepository = myteamChatRepository;
    this.applicationProperties = applicationProperties;
    this.userManager = userManager;
    this.userSearchService = userSearchService;
    this.avatarService = avatarService;
  }

  @Override
  public boolean sendMessage(ApplicationUser user, String message) {
    return sendToApplicationUser(user, message, this::sendMessage);
  }

  @Override
  public boolean sendRawMessage(ApplicationUser user, String message) {
    return sendToApplicationUser(user, message, this::sendRawMessage);
  }

  @Override
  public void sendMessageToUserGroup(String groupName, String message) {
    sendMessageToUserGroup(groupName, message, this::sendMessage);
  }

  @Override
  public void sendRawMessageToUserGroup(String groupName, String message) {
    sendMessageToUserGroup(groupName, message, this::sendRawMessage);
  }

  @Override
  public void sendMessage(String chatId, String message) {
    sendRawOrShieldedMessage(chatId, message, false);
  }

  @Override
  public void sendRawMessage(String chatId, String message) {
    sendRawOrShieldedMessage(chatId, message, true);
  }

  @Nullable
  @Override
  public MyteamChatMetaDto findChatByIssueKey(@Nullable String issueKey) {
    if (StringUtils.isEmpty(issueKey)) {
      throw new IllegalArgumentException("Issue key cannot be null");
    }

    MyteamChatMeta chatByIssueKey = myteamChatRepository.findChatByIssueKey(issueKey);
    if (chatByIssueKey == null) {
      return null;
    }

    return MyteamChatMetaDto.of(
        chatByIssueKey.getID(), chatByIssueKey.getChatId(), chatByIssueKey.getIssueKey());
  }

  @Override
  public void linkChat(@NotNull String chatId, @Nullable final String issueKey)
      throws LinkIssueWithChatException {
    final String validatedIssueKey = validateIssueKey(issueKey);
    myteamChatRepository.persistChat(chatId, validatedIssueKey);
  }

  @Nullable
  @Override
  public ChatMetaDto createChatByJiraApplicationUsers(
      @Nullable final String issueKeyLinkToChat,
      @Nullable final String chatName,
      @Nullable final List<ApplicationUser> jiraUsers,
      @Nullable final ApplicationUser loggedInUser,
      boolean isPublic)
      throws LinkIssueWithChatException {
    if (StringUtils.isEmpty(chatName)) {
      throw new IllegalArgumentException("Chat name cannot be empty string");
    }
    if (loggedInUser == null) {
      throw new IllegalArgumentException("Logged in user cannot be null");
    }
    return createChat(
        issueKeyLinkToChat,
        chatName,
        mapApplicationUsersToChatMembers(jiraUsers),
        loggedInUser,
        isPublic);
  }

  @Nullable
  @Override
  public ChatMetaDto createChatByJiraUserIds(
      @NotNull String issueKeyLinkToChat,
      @NotNull String chatName,
      @NotNull List<Long> memberIds,
      @NotNull ApplicationUser loggedInUser,
      boolean isPublic)
      throws LinkIssueWithChatException {
    return createChat(
        issueKeyLinkToChat,
        chatName,
        mapJiraUserIdToChatMemberId(memberIds),
        loggedInUser,
        isPublic);
  }


  private boolean sendToApplicationUser(final  ApplicationUser user, final  String message, final BiConsumer<String, String> messageSender) {
    if (user == null || StringUtils.isEmpty(message))
      throw new IllegalArgumentException("User and message must be specified");

    String mrimLogin = user.getEmailAddress();
    if (user.isActive() && !StringUtils.isBlank(mrimLogin) && userData.isEnabled(user)) {
      messageSender.accept(mrimLogin, message);
      return true;
    }

    return false;
  }

  private void sendMessageToUserGroup(final String groupName, final String message, final BiConsumer<ApplicationUser, String> messageSender) {
    if (groupName == null || StringUtils.isEmpty(message))
      throw new IllegalArgumentException("Group name and message must be specified");

    Group group = groupManager.getGroup(groupName);
    if (group == null)
      throw new IllegalArgumentException(
              String.format("Group with name %s does not exist", groupName));

    for (ApplicationUser user : groupManager.getUsersInGroup(group)) {
      messageSender.accept(user, message);
    }
  }

  private void sendRawOrShieldedMessage(final String chatId, final  String message, final boolean rawMessage) {
    if (StringUtils.isBlank(message)) {
      throw new IllegalArgumentException("Chat id cannot be empty string");
    }

    if (StringUtils.isBlank(message)) {
      throw new IllegalArgumentException("Message cannot be empty string");
    }

    myteamEventsListener.publishEvent(new JiraNotifyEvent(chatId, rawMessage ? message : Utils.shieldText(message), null));
  }

  @Nullable
  private ChatMetaDto createChat(
      final @Nullable String issueKeyLinkToChat,
      final String chatName,
      final List<ChatMemberId> chatMembers,
      final ApplicationUser loggedInUser,
      final boolean isPublic)
      throws LinkIssueWithChatException {
    String validatedIssueKey = validateIssueKey(issueKeyLinkToChat);
    if (chatMembers.size() >= 30) {
      throw new IllegalArgumentException("User count >= 30");
    }

    try {
      final String about = buildChatAbout(StringUtils.EMPTY, validatedIssueKey);
      final HttpResponse<CreateChatResponse> createChatResponse =
          this.myteamApiClient.createChat(
              pluginData.getToken(), chatName, about, chatMembers, isPublic);
      if (createChatResponse.getStatus() == 200
          && createChatResponse.getBody() != null
          && createChatResponse.getBody().getSn() != null) {
        final String chatId = createChatResponse.getBody().getSn();
        myteamChatRepository.persistChat(chatId, validatedIssueKey);
        sendFirstMessageWithCommandsInCreatedChat(chatId);
        myteamEventsListener.publishEvent(
            new JiraIssueViewEvent(chatId, validatedIssueKey, loggedInUser, true));

        final HttpResponse<ChatInfoResponse> chatInfoResponse = myteamApiClient.getChatInfo(chatId);
        if (chatInfoResponse.getStatus() == 200 && chatInfoResponse.getBody() != null) {
          final List<ApplicationUser> applicationUsers =
              mapChatMembersIdToJiraApplicationUsers(chatMembers);
          final List<ChatMemberDto> chatMemberDtos =
              mapJiraApplicationUsersToChatMemberDto(loggedInUser, applicationUsers);
          final ChatMetaDto chatMetaDto =
              ChatMetaDto.buildChatInfo(chatInfoResponse.getBody(), chatMemberDtos);
          this.myteamApiClient.setAboutChat(
              pluginData.getToken(),
              chatId,
              buildChatAbout(
                  chatMetaDto != null ? chatMetaDto.getLink() : StringUtils.EMPTY,
                  validatedIssueKey));
          return chatMetaDto;
        }
        chatInfoResponse
            .getParsingError()
            .ifPresent(
                e ->
                    SentryClient.capture(
                        e,
                        Map.of(
                            "statusCode",
                            String.valueOf(chatInfoResponse.getStatus()),
                            "dtoClass",
                            ChatInfoResponse.class.getName())));
      }

      createChatResponse
          .getParsingError()
          .ifPresent(
              e ->
                  SentryClient.capture(
                      e,
                      Map.of(
                          "statusCode",
                          String.valueOf(createChatResponse.getStatus()),
                          "chatName",
                          chatName,
                          "about",
                          StringUtils.defaultString(about),
                          "chatMembers",
                          chatMembers.toString(),
                          "publicChat",
                          String.valueOf(isPublic),
                          "dtoClass",
                          CreateChatResponse.class.getName())));

      log.error("Exception during chat creation chat sn not found");
      throw new RuntimeException("Exception during chat creation chat sn not found");
    } catch (IOException | UnirestException | MyteamServerErrorException e) {
      log.error("Exception during chat creation", e);
      throw new RuntimeException("Exception during chat creation", e);
    }
  }

  @NotNull
  private String validateIssueKey(final @Nullable String issueKeyLinkToChat)
      throws LinkIssueWithChatException {
    if (StringUtils.isEmpty(issueKeyLinkToChat)) {
      throw new IllegalArgumentException("Issue key cannot by empty");
    }

    Issue currentIssue = issueManager.getIssueByKeyIgnoreCase(issueKeyLinkToChat);
    // TODO max chat members hardcoded here
    if (currentIssue == null) {
      throw new IssueNotFoundException(
          String.format("Issue not found by key %s", issueKeyLinkToChat));
    }
    if (pluginData.getChatCreationBannedProjectIds().contains(currentIssue.getProjectId())) {
      throw new IllegalArgumentException(
          String.format("Issue %s banned to create chat", issueKeyLinkToChat));
    }

    MyteamChatMetaDto alreadyLinkedIssueToChat = findChatByIssueKey(issueKeyLinkToChat);
    if (alreadyLinkedIssueToChat != null) {
      throw new LinkIssueWithChatException(
          String.format(
              "Issue with key %s already linked to chat with id %s",
              issueKeyLinkToChat, alreadyLinkedIssueToChat.getChatId()));
    }

    return issueKeyLinkToChat;
  }

  @NotNull
  private List<ChatMemberDto> mapJiraApplicationUsersToChatMemberDto(
      @NotNull final ApplicationUser loggedInUser,
      @NotNull List<ApplicationUser> applicationUsers) {
    return applicationUsers.stream()
        .map(
            member -> {
              String url = avatarService.getAvatarURL(loggedInUser, member).toString();
              return new ChatMemberDto(member.getDisplayName(), member.getId(), url);
            })
        .collect(Collectors.toList());
  }

  @NotNull
  private List<ApplicationUser> mapChatMembersIdToJiraApplicationUsers(
      @NotNull final List<ChatMemberId> chatMembers) {
    return chatMembers.stream()
        .map(
            member ->
                StreamSupport.stream(
                        userSearchService.findUsersByEmail(member.getSn()).spliterator(), false)
                    .filter(ApplicationUser::isActive)
                    .findFirst()
                    .orElse(null))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @NotNull
  private List<ChatMemberId> mapJiraUserIdToChatMemberId(@NotNull final List<Long> memberIds) {
    return memberIds.stream()
        .map(memberId -> userManager.getUserById(memberId).orElse(null))
        .filter(Objects::nonNull)
        .filter(userData::isCreateChatsWithUserAllowed)
        .map(user -> new ChatMemberId(user.getEmailAddress()))
        .collect(Collectors.toList());
  }

  @NotNull
  private List<ChatMemberId> mapApplicationUsersToChatMembers(
      @Nullable final List<ApplicationUser> chatMembers) {
    return Optional.ofNullable(chatMembers)
        .map(
            members ->
                members.stream()
                    .filter(Objects::nonNull)
                    .filter(userData::isCreateChatsWithUserAllowed)
                    .map(jiraUser -> new ChatMemberId(jiraUser.getEmailAddress()))
                    .collect(Collectors.toUnmodifiableList()))
        .orElse(Collections.emptyList());
  }

  private void sendFirstMessageWithCommandsInCreatedChat(@NotNull final String chatId) {
    try {
      myteamApiClient.sendMessageText(
          chatId,
          i18nResolver.getRawText(
              "ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.all.commands"));
    } catch (Exception e) {
      log.error("error happened during send message with command in chat", e);
    }
  }

  private String buildChatAbout(@NotNull final String about, @NotNull final String issueKey) {
    return i18nResolver.getText(
        "ru.mail.jira.plugins.myteam.createChat.about.text",
        about,
        applicationProperties.getString(APKeys.JIRA_BASEURL) + "/browse/" + issueKey);
  }
}
