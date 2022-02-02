package ru.mail.jira.plugins.myteam.rulesengine.rules;

import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

public class GroupAdminRule extends BaseRule{
  public GroupAdminRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  public void checkAdminRules() throws AdminRulesRequiredException {
    throw new AdminRulesRequiredException();
  }
}
