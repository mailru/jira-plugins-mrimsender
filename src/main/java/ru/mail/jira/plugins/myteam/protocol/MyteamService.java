/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import com.atlassian.jira.user.ApplicationUser;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.LinkIssueWithChatException;
import ru.mail.jira.plugins.myteam.controller.dto.ChatMetaDto;
import ru.mail.jira.plugins.myteam.service.model.MyteamChatMetaDto;

public interface MyteamService {
  /**
   * Send message to user in Mail.Ru Agent.
   *
   * @param user recipient of the message
   * @param message sent message
   * @return message sent or not
   */
  boolean sendMessage(ApplicationUser user, String message);
  /**
   * Send message to user in Mail.Ru Agent.
   *
   * @param groupName name of Jira group
   * @param message sent message
   */
  void sendMessageToUserGroup(String groupName, String message);
  /**
   * Send message to recipient with id in Mail.Ru Agent.
   *
   * @param chatId recipient of the message
   * @param message sent message
   */
  void sendMessage(String chatId, String message);

  /**
   * Find chat entity in database by issue key. If entity is null then issue with input key has not
   * linked chat in VK Teams
   *
   * @param issueKey issue key to find lined chat to issue entity in database
   * @return possible linked chat to issue
   */
  @Nullable
  MyteamChatMetaDto findChatByIssueKey(@Nullable String issueKey);

  /**
   * Link issue with chat id from VK Teams
   *
   * @param chatId chat id from VK Team
   * @param issueKey issue key of issue to link chat from VK Team
   * @throws LinkIssueWithChatException if issue already linked with chat
   */
  void linkChat(String chatId, String issueKey) throws LinkIssueWithChatException;

  /**
   * Create chat in VK Teams
   *
   * @param jiraUsers chat members (can only allowed to create chat with this user in list)
   * @param chatName chat name
   * @param isPublic flag to allow search chat in VK Teams
   * @param loggedInUser jira user which start action
   * @param issueKeyLinkToChat link issue with this key to created chat
   * @return created chat metadata
   */
  @Nullable
  ChatMetaDto createChatByJiraApplicationUsers(
      @Nullable String issueKeyLinkToChat,
      @Nullable String chatName,
      @Nullable List<ApplicationUser> jiraUsers,
      @Nullable ApplicationUser loggedInUser,
      boolean isPublic)
      throws LinkIssueWithChatException;

  /**
   * Create chat in VK Teams
   *
   * @param memberIds chat members id (can only allowed to create chat with this user in list)
   * @param chatName chat name
   * @param isPublic flag to allow search chat in VK Teams
   * @param loggedInUser jira user which start action
   * @param issueKeyLinkToChat link issue with this key to created chat
   * @return created chat metadata
   */
  @Nullable
  ChatMetaDto createChatByJiraUserIds(
      @NotNull String issueKeyLinkToChat,
      @NotNull String chatName,
      @NotNull List<Long> memberIds,
      @NotNull ApplicationUser loggedInUser,
      boolean isPublic)
      throws LinkIssueWithChatException;
}
