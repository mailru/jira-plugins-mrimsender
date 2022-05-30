/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.rules;

import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

public class ChatAdminRule extends BaseRule {
  public ChatAdminRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  public void checkAdminRules(MyteamEvent event) throws AdminRulesRequiredException {
    if (!userChatService.isChatAdmin(event.getChatId(), event.getUserId())) {
      throw new AdminRulesRequiredException();
    }
  }
}
