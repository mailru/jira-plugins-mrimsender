/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.buttons;

import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "cancel", description = "Clear state and send cancel message")
public class CancelRule extends BaseRule {

  static final ButtonRuleType NAME = ButtonRuleType.Cancel;

  public CancelRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command, @Fact("state") BotState state) {
    return NAME.equalsName(command) && state instanceof CancelableState;
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("state") CancelableState state)
      throws MyteamServerErrorException, IOException {
    state.cancel(event);
  }
}
