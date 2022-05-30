/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.buttons;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.PageableState;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "next page", description = "Update page to next one")
public class NextPageRule extends BaseRule {

  static final RuleType NAME = ButtonRuleType.NextPage;

  public NextPageRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command, @Fact("state") BotState state) {
    return NAME.equalsName(command) && state instanceof PageableState;
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("state") PageableState state) {
    if (state != null) {
      state.nextPage(event);
    }
  }
}
