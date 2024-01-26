Interface MyteamService
=======================
ru.mail.jira.plugins.myteam.protocol.MyteamService
Summary
-------
#### Methods
| Type and modifiers | Method signature                                                                                                                                             | Return type       |
| ------------------ |--------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|
| **public**         | sendMessage(ApplicationUser user,String message)                                                                                                             | boolean           |
| **public**         | sendRawMessage(ApplicationUser user,String message)                                                                                                          | boolean           |
| **public**         | sendMessageToUserGroup(String groupName,String message)                                                                                                      | void              |
| **public**         | sendRawMessageToUserGroup(String groupName,String message)                                                                                                   | void              |
| **public**         | sendMessage(String chatId,String message)                                                                                                                    | void              |
| **public**         | sendRawMessage(String chatId,String message)                                                                                                                 | void              |
| **public**         | findChatByIssueKey(String issueKey)                                                                                                                          | MyteamChatMetaDto |
| **public**         | linkChat(String chatId,String issueKey)                                                                                                                      | void              |
| **public**         | createChatByJiraApplicationUsers(String issueKeyLinkToChat,String chatName, List<ApplicationUser> jiraUsers, ApplicationUser loggedInUser, boolean isPublic) | ChatMetaDto       |
| **public**         | createChatByJiraUserIds(String issueKeyLinkToChat,String chatName, List<Long> memberIds, ApplicationUser loggedInUser, boolean isPublic)                     | ChatMetaDto       |

Methods
=======
sendMessage (ApplicationUser, String)
-------------------------------------
Send shielded message to user in Mail.Ru Agent.
### Parameters
- user: recipient of the message
- message: sent message
### Returns
message sent or not

sendRawMessage (ApplicationUser, String)
-------------------------------------
Send raw message to user in Mail.Ru Agent.
### Parameters
- user: recipient of the message
- message: sent message
### Returns
message sent or not

sendMessageToUserGroup (String, String)
-------------------------------------
Send shielded message to user in Mail.Ru Agent.
### Parameters
- groupName: name of Jira group
- message: sent message

sendRawMessageToUserGroup (String, String)
-------------------------------------
Send raw message to user in Mail.Ru Agent.
### Parameters
- groupName: name of Jira group
- message: sent message

sendMessage (String, String)
----------------------------
Send shielded message to recipient with id in Mail.Ru Agent.
### Parameters
- chatId: recipient of the message
- message: sent message

sendRawMessage (String, String)
----------------------------
Send raw message to recipient with id in Mail.Ru Agent.
### Parameters
- chatId: recipient of the message
- message: sent message

findChatByIssueKey (String)
----------------------------
Find chat entity in database by issue key. If entity is null then issue with input key has not linked chat in VK Teams
### Parameters
- issueKey: issue key to find lined chat to issue entity in database
### Returns
possible linked chat to issue

linkChat (String, String)
----------------------------
Link issue with chat id from VK Teams
### Parameters
- chatId: chat id from VK Team
- issueKey: issue key of issue to link chat from VK Team
### Returns
possible linked chat to issue
### Throws
if issue already to chat

createChatByJiraApplicationUsers (List<com.atlassian.jira.user.ApplicationUser>, String, String, boolean, String)
----------------------------
Create chat in VK Teams
### Parameters
- jiraUserIds: chat members id (can only allowed to create chat with this user in list)
- chatName: chat name
- isPublic: flag to allow search chat in VK Teams
- loggedInUser: jira user which start action
- issueKeyLinkToChat: link issue with this key to created chat
### Returns
created chat metadata

createChatByJiraUserIds (List<java.lang.Long>, String, String, boolean, String)
----------------------------
Create chat in VK Teams
### Parameters
- memberIds: chat members id (can only allowed to create chat with this user in list)
- chatName: chat name
- isPublic: flag to allow search chat in VK Teams
- loggedInUser: jira user which start action
- issueKeyLinkToChat: link issue with this key to created chat
### Returns
created chat metadata


