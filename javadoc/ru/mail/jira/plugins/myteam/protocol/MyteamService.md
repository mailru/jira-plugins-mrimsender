Interface MyteamService
=======================
ru.mail.jira.plugins.myteam.protocol.MyteamService
Summary
-------
#### Methods
| Type and modifiers | Method signature                                 | Return type |
| ------------------ | ------------------------------------------------ | ----------- |
| **public**         | sendMessage(ApplicationUser user,String message) | boolean     |
| **public**         | sendMessage(String chatId,String message)        | void        |

Methods
=======
sendMessage (ApplicationUser, String)
-------------------------------------
Send message to user in Mail.Ru Agent.
### Parameters
- user: recipient of the message
- message: sent message
### Returns
message sent or not

sendMessage (String, String)
----------------------------
Send message to recipient with id in Mail.Ru Agent.
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

createChat (List, String, String, boolean, String)
----------------------------
Send message to recipient with id in Mail.Ru Agent.
### Parameters
- chatMembers: initial member in created chat
- chatName: chat name
- about: chat description
- publicChat: public or not
- issueKeyLinkToChat: issue key to link to created chat
### Returns
created and linked chat to issue



