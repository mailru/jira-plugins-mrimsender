/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.Locale;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;

@Service
public class UserChatServiceImpl implements UserChatService {

  private final MyteamApiClient myteamApiClient;
  private final UserData userData;
  private final LocaleManager localeManager;
  private final I18nResolver i18nResolver;

  public UserChatServiceImpl(
      MyteamApiClient myteamApiClient,
      UserData userData,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport I18nResolver i18nResolver) {
    this.myteamApiClient = myteamApiClient;
    this.userData = userData;
    this.localeManager = localeManager;
    this.i18nResolver = i18nResolver;
  }

  @Override
  public ApplicationUser getJiraUserFromUserChatId(String id) {
    return userData.getUserByMrimLogin(id);
  }

  @Override
  public Locale getUserLocale(ApplicationUser user) {
    return localeManager.getLocaleFor(user);
  }

  @Override
  public MyteamApiClient getMyTeamClient() {
    return myteamApiClient;
  }

  @Override
  public I18nResolver getI18nResolver() {
    return i18nResolver;
  }
}
