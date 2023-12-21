/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import com.atlassian.jira.user.ApplicationUser;
import java.util.List;
import org.jetbrains.annotations.Nullable;
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
   * Create chat in VK Teams
   *
   * @param chatMembers initial chat members (can only active and allowed to create chat with this
   *     user in list)
   * @param chatName chat name
   * @param publicChat flag to allow search chat in VK Teams
   * @param issueKeyLinkToChat link issue with this key to created chat
   * @return created and linked chat to issue
   */
  @Nullable
  MyteamChatMetaDto createChat(
      @Nullable List<ApplicationUser> chatMembers,
      @Nullable String chatName,
      @Nullable String about,
      boolean publicChat,
      @Nullable String issueKeyLinkToChat);
}
