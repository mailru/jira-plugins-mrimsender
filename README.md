mrimsender
==========

VK Teams Notifications JIRA Plugin

## Plugin pages
- Bot configuration - https://{jiraUrl}/secure/MyteamConfiguration!default.jspa
- Subscriptions - https://{jiraUrl}/myteam/subscriptions 
- Issue creation by reply - https://{jiraUrl}/myteam/projects/{projectKey}/settings/chats
- Access request configuration - https://{jiraUrl}/secure/AccessRequestConfiguration.jspa?project={projectKey}

## API service for groovy scripts

ru.mail.jira.plugins.myteam.protocol.MyteamService

```java

package ru.mail.jira.plugins.myteam.protocol;

import com.atlassian.jira.user.ApplicationUser;

public interface MyteamService {
  /**
   * Send message to user in VK Teams.
   *
   * @param user recipient of the message
   * @param message sent message
   * @return message sent or not
   */
  boolean sendMessage(ApplicationUser user, String message);
  /**
   * Send message to user in VK Teams.
   *
   * @param groupName name of Jira group
   * @param message sent message
   */
  void sendMessageToUserGroup(String groupName, String message);
  /**
   * Send message to recipient with id in VK Teams.
   *
   * @param chatId recipient of the message
   * @param message sent message
   */
  void sendMessage(String chatId, String message);

    /**
     * Find chat entity in database by issue key.
     * If entity is null then issue with input key has not linked chat in VK Teams
     * @param issueKey recipient of the message
     */
    @Nullable
    MyteamChatMetaDto findChatByIssueKey(@Nullable String issueKey);

    /**
     * Create chat in VK Teams
     * @param chatMembers initial chat members (can only active and allowed to create chat with this user in list)
     * @param chatName chat name
     * @param about chat description
     * @param publicChat flag to allow search chat in VK Teams
     * @param issueKeyLinkToChat link issue with this key to created chat
     */
    @Nullable
    MyteamChatMetaDto createChat(@Nullable List<ApplicationUser> chatMembers,
                                 @Nullable String chatName,
                                 @Nullable String about,
                                 boolean publicChat,
                                 @Nullable String issueKeyLinkToChat);
}
```