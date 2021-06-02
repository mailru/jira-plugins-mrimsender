Class ExternalSystemNotificationsService
========================================
ru.mail.jira.plugins.myteam.rest.ExternalSystemNotificationsService
Summary
-------
#### Constructors
| Visibility | Signature                                                                                                                                            |
| ---------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| **public** | ExternalSystemNotificationsService(ApplicationLinkService, JiraAuthenticationContext, UserManager, MessageFormatter, UserData, MyteamEventsListener) |
#### Methods
| Type and modifiers | Method signature                                                                                                             | Return type |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------- | ----------- |
| **public**         | bitbucketProjectEventsWebHook(BitbucketEventDto event)                                                                       | Void        |
| **public**         | getAllBitbucketRepositoryWatchers()                                                                                          | List        |
| **public**         | sendMyteamNotifications(java.util.stream.Stream<com.atlassian.jira.user.ApplicationUser> recipients,BitbucketEventDto event) | void        |

Constructors
============
ExternalSystemNotificationsService (ApplicationLinkService, JiraAuthenticationContext, UserManager, MessageFormatter, UserData, MyteamEventsListener)
-----------------------------------------------------------------------------------------------------------------------------------------------------


Methods
=======
bitbucketProjectEventsWebHook (BitbucketEventDto)
-------------------------------------------------
No method description provided

getAllBitbucketRepositoryWatchers ()
------------------------------------
No method description provided

sendMyteamNotifications (Stream<ApplicationUser>, BitbucketEventDto)
--------------------------------------------------------------------
No method description provided


