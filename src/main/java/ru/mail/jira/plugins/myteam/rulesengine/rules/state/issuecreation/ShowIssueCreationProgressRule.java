/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.CreatingIssueState;

@Rule(
    name = "show issue creation message",
    description = "Calls to show filled and current filling fields")
public class ShowIssueCreationProgressRule extends BaseRule {

  static final RuleType NAME = StateActionRuleType.ShowCreatingIssueProgressMessage;

  public ShowIssueCreationProgressRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("state") BotState state, @Fact("command") String command) {
    return state instanceof CreatingIssueState && NAME.equalsName(command);
  }

  @Action
  public void execute(
      @Fact("event") MyteamEvent event,
      @Fact("state") CreatingIssueState state,
      @Fact("args") String value)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    String chatId = event.getChatId();
    if (user != null) {
      Locale locale = userChatService.getUserLocale(user);

      Optional<Field> currentField = state.getFirstUnfilledField();

      if (currentField.isPresent()) {
        state.setFieldValue(currentField.get(), value);
        userChatService.sendMessageText(
            chatId,
            state.createInsertFieldMessage(locale, currentField.get()),
            messageFormatter.buildButtonsWithCancel(
                null,
                userChatService.getRawText(
                    locale,
                    "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
      } else {
        userChatService.sendMessageText(chatId, "FILLED ALL"); // TODO show issue creation confirm
      }
    }
  }
}
