/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.buttons;

import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.CancelableState;

@Rule(name = "cancel", description = "Clear state and send cancel message")
public class CancelRule extends BaseRule {

  static final ButtonRuleType NAME = ButtonRuleType.Cancel;

  public CancelRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command, @Fact("event") MyteamEvent event) {
    BotState state = userChatService.getState(event.getChatId());
    return NAME.equalsName(command) && state instanceof CancelableState;
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("state") CancelableState state)
      throws MyteamServerErrorException, IOException {
    userChatService.deleteState(event.getChatId());
    if (event instanceof ButtonClickEvent) {
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
      userChatService.editMessageText(
          event.getChatId(), ((ButtonClickEvent) event).getMsgId(), state.getCancelMessage(), null);
    } else {
      userChatService.sendMessageText(event.getChatId(), state.getCancelMessage());
    }
  }
}
