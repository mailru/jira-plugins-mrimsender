/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models;

import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

public class BaseRule {
  protected final MessageFormatter messageFormatter;
  protected final UserChatService userChatService;

  public BaseRule(UserChatService userChatService) {
    this.userChatService = userChatService;
    messageFormatter = userChatService.getMessageFormatter();
  }
}
