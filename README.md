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
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.LinkIssueWithChatException;

public interface MyteamService {
    /**
     * Send shielded message to user in VK Teams.
     *
     * @param user recipient of the message
     * @param message sent message
     * @return message sent or not
     */
    boolean sendMessage(ApplicationUser user, String message);

    /**
     * Send raw message to user in Mail.Ru Agent.
     *
     * @param user recipient of the message
     * @param message sent message
     * @return message sent or not
     */
    boolean sendRawMessage(ApplicationUser user, String message);

    /**
     * Send shielded message to user in VK Teams.
     *
     * @param groupName name of Jira group
     * @param message sent message
     */
    void sendMessageToUserGroup(String groupName, String message);

    /**
     * Send raw message to user in Mail.Ru Agent.
     *
     * @param groupName name of Jira group
     * @param message sent message
     */
    void sendRawMessageToUserGroup(String groupName, String message);

    /**
     * Send shielded message to recipient with id in VK Teams.
     *
     * @param chatId recipient of the message
     * @param message sent message
     */
    void sendMessage(String chatId, String message);

    /**
     * Send raw message to recipient with id in Mail.Ru Agent.
     *
     * @param chatId recipient of the message
     * @param message sent message
     */
    void sendRawMessage(String chatId, String message);

    /**
     * Find chat entity in database by issue key.
     * If entity is null then issue with input key has not linked chat in VK Teams
     * @param issueKey recipient of the message
     */
    @Nullable
    MyteamChatMetaDto findChatByIssueKey(@Nullable String issueKey);

    /**
     * Link issue with chat id from VK Teams
     * @param chatId chat id from VK Team
     * @param issueKey issue key of issue to link chat from VK Team
     * @throws LinkIssueWithChatException if issue already linked with chat
     */
    void linkChat(String chatId, String issueKey) throws LinkIssueWithChatException;

    /**
     * Create chat in VK Teams
     * @param jiraUsers chat members (can only allowed to create chat with this user in list)
     * @param chatName chat name
     * @param isPublic flag to allow search chat in VK Teams
     * @param loggedInUser jira user which start action
     * @param issueKeyLinkToChat link issue with this key to created chat
     * @return created chat metadata
     */
    @Nullable
    MyteamChatMetaDto createChatByJiraApplicationUsers(@Nullable List<ApplicationUser> jiraUsers,
                                                       @Nullable String chatName,
                                                       @Nullable String about,
                                                       @Nullable ApplicationUser loggedInUser,
                                                       boolean isPublic,
                                                       @Nullable String issueKeyLinkToChat);

    /**
     * Create chat in VK Teams
     * @param jiraUserIds chat members id (can only allowed to create chat with this user in list)
     * @param chatName chat name
     * @param isPublic flag to allow search chat in VK Teams
     * @param loggedInUser jira user which start action
     * @param issueKeyLinkToChat link issue with this key to created chat
     * @return created chat metadata
     */
    @Nullable
    MyteamChatMetaDto createChatByJiraUserIds(@Nullable List<Long> jiraUserIds,
                                              @Nullable String chatName,
                                              @Nullable String about,
                                              @Nullable ApplicationUser loggedInUser,
                                              boolean isPublic,
                                              @Nullable String issueKeyLinkToChat);
}
```