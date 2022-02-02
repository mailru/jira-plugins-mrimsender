/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuesearch;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.ViewingIssueState;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;

@Rule(name = "issue key input result", description = "Fired when waiting for issue key on input")
public class IssueKeyInputRule extends BaseRule {

  public IssueKeyInputRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("state") BotState state, @Fact("args") String issueKey) {
    return state instanceof ViewingIssueState
        && (((ViewingIssueState) state).getIssueKey() == null
            || ((ViewingIssueState) state).getIssueKey().length() == 0)
        && issueKey != null
        && issueKey.length() > 0;
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String issueKey) {
    rulesEngine.fireCommand(CommandRuleType.Issue, event, issueKey);
  }
}
