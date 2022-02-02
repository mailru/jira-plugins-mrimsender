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
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.CreatingIssueState;

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
    return state instanceof CreatingIssueState && NAME.equalsName(command);
  }

  @Action
  public void execute(
      @Fact("event") MyteamEvent event,
      @Fact("state") CreatingIssueState state,
      @Fact("args") String value)
      throws MyteamServerErrorException, IOException {
    Optional<Field> field = state.getCurrentField();
    if (event instanceof ButtonClickEvent)
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
    if (field.isPresent()) {
      CreateIssueFieldValueHandler handler = issueCreationService.getFieldValueHandler(field.get());
      state.setCurrentFieldValue(handler.updateValue(state.getFieldValue(field.get()), value));
      state.nextField(true);
      rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
    }
  }
}
