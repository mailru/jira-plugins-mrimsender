/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.Locale;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;

public interface UserChatService {

  ApplicationUser getJiraUserFromUserChatId(String id);

  Locale getUserLocale(ApplicationUser user);

  MyteamApiClient getMyteamClient();

  I18nResolver getI18nResolver();

  MessageFormatter getMessageFormatter();

  String getJiraBaseUrl();
}
