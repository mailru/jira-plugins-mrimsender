/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.commands;

import com.atlassian.sal.api.message.I18nResolver;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.rulesengine.UserChatService;

public class BaseCommandRule {

  protected final I18nResolver i18nResolver;
  protected final MyteamApiClient myteamClient;
  protected final UserChatService userChatService;

  public BaseCommandRule(UserChatService userChatService) {
    this.userChatService = userChatService;
    this.myteamClient = userChatService.getMyTeamClient();
    this.i18nResolver = userChatService.getI18nResolver();
  }
}
