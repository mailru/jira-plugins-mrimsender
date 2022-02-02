/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.buttons;

import com.atlassian.crowd.exception.UserNotFoundException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Rule(name = "search by input jql", description = "Shows issues by JQL input")
public class SearchIssueByJqlInputRule extends BaseRule {

  static final RuleType NAME = ButtonRuleType.SearchIssueByJqlInput;

  public SearchIssueByJqlInputRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event) throws UserNotFoundException {
    rulesEngine.fireCommand(CommandRuleType.SearchByJql, event);
  }
}
