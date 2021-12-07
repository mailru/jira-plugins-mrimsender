/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.buttons;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.PageableState;

@Rule(name = "prev page", description = "Update page to previous one")
public class PrevPageRule extends BaseRule {

  static final ButtonRuleType NAME = ButtonRuleType.PrevPage;

  public PrevPageRule(UserChatService userChatService) {
    super(userChatService);
  }

  @Condition
  public boolean isValid(@Fact("command") String command, @Fact("event") MyteamEvent event) {
    BotState state = userChatService.getState(event.getChatId());
    return NAME.equalsName(command) && state instanceof PageableState;
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event) {
    PageableState state = (PageableState) userChatService.getState(event.getChatId());

    if (state != null) {
      state.prevPage((ButtonClickEvent) event);
    }
  }
}