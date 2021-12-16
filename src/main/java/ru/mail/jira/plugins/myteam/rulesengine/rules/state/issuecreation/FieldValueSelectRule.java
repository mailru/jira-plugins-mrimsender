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
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueCreationFieldsService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.CreatingIssueState;

@Rule(
    name = "select issue creation value",
    description = "Calls when issue field value was selected")
public class FieldValueSelectRule extends BaseRule {

  static final RuleType NAME = ButtonRuleType.SelectIssueCreationValue;

  private final IssueCreationFieldsService issueCreationFieldsService;

  public FieldValueSelectRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueCreationFieldsService issueCreationFieldsService) {
    super(userChatService, rulesEngine);
    this.issueCreationFieldsService = issueCreationFieldsService;
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
    Optional<Field> field = state.getCurrentField();
    if (field.isPresent()) {
      // TODO add field value validation

      CreateIssueFieldValueHandler handler =
          issueCreationFieldsService.getFieldValueHandler(field.get());
      state.setCurrentFieldValue(handler.updateValue(state.getFieldValue(field.get()), value));
      state.nextField();
      if (event instanceof ButtonClickEvent)
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
      rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, state, event);
    }
  }
}
