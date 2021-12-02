/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.Locale;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;

public interface UserChatService {

  ApplicationUser getJiraUserFromUserChatId(String id);

  Locale getUserLocale(ApplicationUser user);

  MyteamApiClient getMyTeamClient();

  I18nResolver getI18nResolver();
}
