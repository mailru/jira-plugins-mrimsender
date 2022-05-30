/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules;

import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

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
