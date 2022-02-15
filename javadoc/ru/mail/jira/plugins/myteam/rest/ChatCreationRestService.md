Class ChatCreationRestService
=============================
ru.mail.jira.plugins.myteam.controller.ChatCreationController
Summary
-------
#### Constructors
| Visibility | Signature                                                                                                                                                                                                                                                              |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **public** | ChatCreationRestService(JiraAuthenticationContext, IssueManager, WatcherManager, AvatarService, UserManager, UserSearchService, I18nResolver, LocaleManager, ApplicationProperties, MyteamApiClient, MyteamChatRepository, PluginData, UserData, MyteamEventsListener) |
#### Methods
| Type and modifiers | Method signature                                                                     | Return type         |
| ------------------ | ------------------------------------------------------------------------------------ | ------------------- |
| **public**         | findChatData(String issueKey)                                                        | ChatMetaDto         |
| **public**         | getChatCreationData(String issueKey)                                                 | ChatCreationDataDto |
| **public**         | createChat(String issueKey,String chatName,java.util.List<java.lang.Long> memberIds) | ChatMetaDto         |
| **public**         | getAvailableChatMembers(String searchText)                                           | List                |

Constructors
============
ChatCreationRestService (JiraAuthenticationContext, IssueManager, WatcherManager, AvatarService, UserManager, UserSearchService, I18nResolver, LocaleManager, ApplicationProperties, MyteamApiClient, MyteamChatRepository, PluginData, UserData, MyteamEventsListener)
-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


Methods
=======
findChatData (String)
---------------------
No method description provided

getChatCreationData (String)
----------------------------
No method description provided

createChat (String, String, List<Long>)
---------------------------------------
No method description provided

getAvailableChatMembers (String)
--------------------------------
No method description provided


