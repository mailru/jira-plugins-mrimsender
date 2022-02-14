/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.jqlsearch;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.states.JqlSearchState;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "jql input result", description = "Fired when waiting for jql on input")
public class JqlInputRule extends BaseRule {

  public JqlInputRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("state") BotState state, @Fact("args") String jql) {
    return state instanceof JqlSearchState
        && ((JqlSearchState) state).getJql().length() == 0
        && jql != null
        && jql.length() > 0;
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String jql) {
    rulesEngine.fireCommand(CommandRuleType.SearchByJql, event, jql);
  }
}
