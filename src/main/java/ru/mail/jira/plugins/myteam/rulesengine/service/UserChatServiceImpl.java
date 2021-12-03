/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.Locale;
import lombok.Getter;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;

@Service
public class UserChatServiceImpl implements UserChatService {

  private final UserData userData;
  private final LocaleManager localeManager;
  private final String JIRA_BASE_URL;

  @Getter(onMethod_ = {@Override})
  private final MyteamApiClient myteamClient;

  @Getter(onMethod_ = {@Override})
  private final I18nResolver i18nResolver;

  @Getter(onMethod_ = {@Override})
  private final MessageFormatter messageFormatter;

  public UserChatServiceImpl(
      MyteamApiClient myteamApiClient,
      UserData userData,
      MessageFormatter messageFormatter,
      @ComponentImport LocaleManager localeManager,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport ApplicationProperties applicationProperties) {
    this.myteamClient = myteamApiClient;
    this.userData = userData;
    this.localeManager = localeManager;
    this.i18nResolver = i18nResolver;
    this.messageFormatter = messageFormatter;
    this.JIRA_BASE_URL = applicationProperties.getString(APKeys.JIRA_BASEURL);
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
  public String getJiraBaseUrl() {
    return JIRA_BASE_URL;
  }
}
