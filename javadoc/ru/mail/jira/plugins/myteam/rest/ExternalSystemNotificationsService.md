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
| Type and modifiers | Method signature                                                                                                             | Return type               |
| ------------------ | ---------------------------------------------------------------------------------------------------------------------------- | ------------------------- |
| **public**         | bitbucketProjectEventsWebHook(BitbucketEventDto event)                                                                       | BitbucketWebhookResultDto |
| **public**         | getAllBitbucketRepositoryWatchers(String projectKey,String repositorySlug)                                                   | List                      |
| **public**         | sendMyteamNotifications(java.util.stream.Stream<com.atlassian.jira.user.ApplicationUser> recipients,BitbucketEventDto event) | void                      |

Constructors
============
ExternalSystemNotificationsService (ApplicationLinkService, JiraAuthenticationContext, UserManager, MessageFormatter, UserData, MyteamEventsListener)
-----------------------------------------------------------------------------------------------------------------------------------------------------


Methods
=======
bitbucketProjectEventsWebHook (BitbucketEventDto)
-------------------------------------------------
No method description provided

getAllBitbucketRepositoryWatchers (String, String)
--------------------------------------------------
No method description provided

sendMyteamNotifications (Stream<ApplicationUser>, BitbucketEventDto)
--------------------------------------------------------------------
No method description provided


