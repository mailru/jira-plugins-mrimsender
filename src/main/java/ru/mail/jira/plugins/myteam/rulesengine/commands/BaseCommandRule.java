/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.commands;

import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

public class BaseCommandRule {
  protected final MessageFormatter messageFormatter;
  protected final UserChatService userChatService;

  public BaseCommandRule(UserChatService userChatService) {
    this.userChatService = userChatService;
    messageFormatter = userChatService.getMessageFormatter();
  }
}
