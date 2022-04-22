/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

import com.atlassian.jira.issue.fields.Field;
import java.io.IOException;
import java.util.Optional;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.configuration.createissue.customfields.CreateIssueFieldValueHandler;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.CreatingIssueState;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.FillingIssueFieldState;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(
    name = "select issue creation value",
    description = "Calls when issue field value was selected")
public class FieldValueSelectRule extends BaseRule {

  static final RuleType NAME = StateActionRuleType.SelectIssueCreationValue;

  private final IssueCreationService issueCreationService;

  public FieldValueSelectRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueCreationService issueCreationService) {
    super(userChatService, rulesEngine);
    this.issueCreationService = issueCreationService;
  }

  @Condition
  public boolean isValid(@Fact("state") BotState state, @Fact("command") String command) {
    return state instanceof FillingIssueFieldState && NAME.equalsName(command);
  }

  @Action
  public void execute(
      @Fact("event") MyteamEvent event,
      @Fact("state") FillingIssueFieldState state,
      @Fact("args") String value)
      throws MyteamServerErrorException, IOException {
    Field field = state.getField();
    if (event instanceof ButtonClickEvent) {
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
    }

    CreateIssueFieldValueHandler handler = issueCreationService.getFieldValueHandler(field);

    BotState prevState = userChatService.getPrevState(event.getChatId());

    userChatService.revertState(event.getChatId());

    if (prevState instanceof CreatingIssueState) {
      ((CreatingIssueState) prevState)
          .setFieldValue(field, handler.updateValue(state.getValue(), value));
      ((CreatingIssueState) prevState).nextField(true);

      Optional<Field> lastField = ((CreatingIssueState) prevState).getCurrentField();
      if (lastField.isPresent()) {
        FillingIssueFieldState fillingFieldState =
            new FillingIssueFieldState(
                userChatService, rulesEngine, lastField.get(), handler.isSearchable(), false);
        userChatService.setState(event.getChatId(), fillingFieldState);
      }
    }

    rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
  }
}
