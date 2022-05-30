/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.FillingIssueFieldState;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "field input rule", description = "Fires on field value input while creating issue")
public class FieldInputRule extends BaseRule {

  public FieldInputRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("state") BotState state, @Fact("args") String value) {
    return state instanceof FillingIssueFieldState && value != null && value.length() > 0;
  }

  @Action
  public void execute(
      @Fact("state") FillingIssueFieldState state,
      @Fact("event") ChatMessageEvent event,
      @Fact("args") String value)
      throws MyteamServerErrorException, IOException {
    state.setInput(value);

    if (state.isSearchOn()) {
      state.getPager().setPage(0);
      state.getPager().setTotal(0);
      rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event, value);
    } else {
      rulesEngine.fireCommand(StateActionRuleType.SelectIssueCreationValue, event, value);
    }
  }
}
