/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "/assigned", description = "Shows user's active assigned issues")
public class AssignedIssuesCommandRule extends BaseRule {
  static final RuleType NAME = CommandRuleType.AssignedIssues;

  public AssignedIssuesCommandRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event) {
    rulesEngine.fireCommand(
        CommandRuleType.SearchByJql,
        event,
        "assignee = currentUser() AND resolution = Unresolved ORDER BY updated");
  }
}
