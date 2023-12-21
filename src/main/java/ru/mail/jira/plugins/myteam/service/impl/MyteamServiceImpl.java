/* (C)2020 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMemberId;
import ru.mail.jira.plugins.myteam.protocol.MyteamService;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.UserChatService;
import ru.mail.jira.plugins.myteam.service.model.MyteamChatMetaDto;

@Service
@Slf4j
public class MyteamServiceImpl implements MyteamService {
  private final GroupManager groupManager;
  private final UserData userData;
  private final MyteamEventsListener myteamEventsListener;

  private final UserChatService userChatService;

  private final IssueService issueService;

  @Autowired
  public MyteamServiceImpl(
      @ComponentImport GroupManager groupManager,
      MyteamEventsListener myteamEventsListener,
      UserData userData,
      UserChatService userChatService,
      IssueService issueService) {
    this.groupManager = groupManager;
    this.userData = userData;
    this.myteamEventsListener = myteamEventsListener;
    this.userChatService = userChatService;
    this.issueService = issueService;
  }

  @Override
  public boolean sendMessage(ApplicationUser user, String message) {
    if (user == null || StringUtils.isEmpty(message))
      throw new IllegalArgumentException("User and message must be specified");

    String mrimLogin = user.getEmailAddress();
    if (user.isActive() && !StringUtils.isBlank(mrimLogin) && userData.isEnabled(user)) {
      sendMessage(mrimLogin, message);
      return true;
    }
    return false;
  }

  @Override
  public void sendMessageToUserGroup(String groupName, String message) {
    if (groupName == null || StringUtils.isEmpty(message))
      throw new IllegalArgumentException("Group name and message must be specified");

    Group group = groupManager.getGroup(groupName);
    if (group == null)
      throw new IllegalArgumentException(
          String.format("Group with name %s does not exist", groupName));

    for (ApplicationUser user : groupManager.getUsersInGroup(group)) {
      sendMessage(user, message);
    }
  }

  @Override
  public void sendMessage(String chatId, String message) {
    myteamEventsListener.publishEvent(new JiraNotifyEvent(chatId, Utils.shieldText(message), null));
  }

  @Nullable
  @Override
  public MyteamChatMetaDto findChatByIssueKey(@Nullable String issueKey) {
    if (StringUtils.isEmpty(issueKey)) {
      throw new IllegalArgumentException("Issue key cannot be null");
    }

    return userChatService.findChatByIssueKey(issueKey);
  }

  @Nullable
  @Override
  public MyteamChatMetaDto createChat(
      @Nullable final List<ApplicationUser> chatMembers,
      @Nullable final String chatName,
      @Nullable final String about,
      final boolean publicChat,
      @Nullable final String issueKeyLinkToChat) {
    if (StringUtils.isEmpty(issueKeyLinkToChat)) {
      throw new IllegalArgumentException("Issue key for link to chat name cannot be empty string");
    }

    validateIssueKey(issueKeyLinkToChat);

    if (StringUtils.isEmpty(chatName)) {
      throw new IllegalArgumentException("Chat name cannot be empty string");
    }

    if (chatMembers == null) {
      throw new IllegalArgumentException("List of user to add in creating chat cannot be null");
    }

    return tryCreateChat(chatMembers, chatName, about, publicChat, issueKeyLinkToChat);
  }

  @Nullable
  private MyteamChatMetaDto tryCreateChat(
      @NotNull List<ApplicationUser> chatMembers,
      @NotNull String chatName,
      @Nullable String about,
      boolean publicChat,
      @NotNull String issueKeyLinkToChat) {
    final List<ChatMemberId> members = mapApplicationUsersToChatMembers(chatMembers);
    if (members.size() != 0) {
      try {
        String chatId = userChatService.createChat(chatName, about, members, publicChat);
        return userChatService.unsafeLinkChat(chatId, issueKeyLinkToChat);
      } catch (Exception e) {
        log.error(
            String.format(
                "Error happened during create chat by params - chatMembers: %s, chatName: %s, publicChat: %s, issueKeyLinkToChat: %s",
                chatMembers, chatMembers, publicChat, issueKeyLinkToChat),
            e);
        SentryClient.capture(
            e,
            Map.of(
                "issueKey",
                issueKeyLinkToChat,
                "chatName",
                chatName,
                "publicChat:",
                String.valueOf(publicChat),
                "chatMembers",
                members.toString()));
        return null;
      }
    } else {
      throw new IllegalArgumentException(
          String.format("Chat cannot be created with users %s", chatMembers));
    }
  }

  private void validateIssueKey(@NotNull String issueKeyLinkToChat) {
    issueService.getIssue(issueKeyLinkToChat);

    MyteamChatMetaDto alreadyLinkedIssueToChat = findChatByIssueKey(issueKeyLinkToChat);
    if (alreadyLinkedIssueToChat != null) {
      throw new IllegalArgumentException(
          String.format(
              "Issue with key %s already linked to chat with id %s",
              issueKeyLinkToChat, alreadyLinkedIssueToChat.getChatId()));
    }
  }

  @NotNull
  private List<ChatMemberId> mapApplicationUsersToChatMembers(
      @NotNull final List<ApplicationUser> chatMembers) {
    return chatMembers.stream()
        .filter(Objects::nonNull)
        .filter(ApplicationUser::isActive)
        .filter(userData::isCreateChatsWithUserAllowed)
        .map(jiraUser -> new ChatMemberId(jiraUser.getEmailAddress()))
        .collect(Collectors.toUnmodifiableList());
  }
}
