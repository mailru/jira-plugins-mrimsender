Interface MyteamService
=======================
ru.mail.jira.plugins.myteam.service.MyteamService
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


