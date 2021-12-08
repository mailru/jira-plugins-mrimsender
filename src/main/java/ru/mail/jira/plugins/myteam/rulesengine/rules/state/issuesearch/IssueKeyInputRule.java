/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuesearch;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.MyteamRulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.ViewingIssueState;

@Rule(name = "issue key input result", description = "Fired when waiting for issue key on input")
public class IssueKeyInputRule extends BaseRule {

  public IssueKeyInputRule(UserChatService userChatService) {
    super(userChatService);
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
    Facts facts = MyteamRulesEngine.formCommandFacts(CommandRuleType.Issue, event, issueKey);
    userChatService.fireRule(facts);
  }
}
