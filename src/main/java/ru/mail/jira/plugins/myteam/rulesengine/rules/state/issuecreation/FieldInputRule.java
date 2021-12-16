/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.CreatingIssueState;

@Rule(name = "field input rule", description = "Fires on field value input while creating issue")
public class FieldInputRule extends BaseRule {

  public FieldInputRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("state") BotState state, @Fact("args") String value) {
    return state instanceof CreatingIssueState
        && ((CreatingIssueState) state).getIssueType() != null
        && ((CreatingIssueState) state).getProject() != null
        && value != null
        && value.length() > 0;
  }

  @Action
  public void execute(
      @Fact("event") ChatMessageEvent event,
      @Fact("state") CreatingIssueState state,
      @Fact("args") String value)
      throws MyteamServerErrorException, IOException {
    rulesEngine.fireCommand(ButtonRuleType.SelectIssueCreationValue, state, event, value);
  }
}
