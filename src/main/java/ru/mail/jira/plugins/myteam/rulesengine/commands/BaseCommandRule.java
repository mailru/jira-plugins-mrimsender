/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.commands;

import com.atlassian.sal.api.message.I18nResolver;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

public class BaseCommandRule {
  protected final MessageFormatter messageFormatter;
  protected final I18nResolver i18nResolver;
  protected final MyteamApiClient myteamClient;
  protected final UserChatService userChatService;

  public BaseCommandRule(UserChatService userChatService) {
    this.userChatService = userChatService;
    myteamClient = userChatService.getMyteamClient();
    i18nResolver = userChatService.getI18nResolver();
    messageFormatter = userChatService.getMessageFormatter();
  }
}
