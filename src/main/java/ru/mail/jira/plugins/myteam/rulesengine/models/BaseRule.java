/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models;

import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

public class BaseRule {
  protected final MessageFormatter messageFormatter;
  protected final RulesEngine rulesEngine;
  protected final UserChatService userChatService;

  public BaseRule(UserChatService userChatService, RulesEngine rulesEngine) {
    this.userChatService = userChatService;
    this.rulesEngine = rulesEngine;
    messageFormatter = userChatService.getMessageFormatter();
  }
}
