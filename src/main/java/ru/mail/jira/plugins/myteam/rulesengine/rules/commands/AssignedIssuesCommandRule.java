/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.commands;

import com.atlassian.jira.user.ApplicationUser;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.MyteamRulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ServiceRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Rule(name = "/assigned", description = "Shows user's active assigned issues")
public class AssignedIssuesCommandRule extends BaseRule {
  static final RuleType NAME = CommandRuleType.AssignedIssues;

  public AssignedIssuesCommandRule(UserChatService userChatService) {
    super(userChatService);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event) {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (user != null) {
      Facts facts = MyteamRulesEngine.formBasicsFacts(ServiceRuleType.SearchByJql, event);
      facts.put("args", "assignee = currentUser() AND resolution = Unresolved ORDER BY updated");
      userChatService.fireRule(facts);
    }
  }
}
