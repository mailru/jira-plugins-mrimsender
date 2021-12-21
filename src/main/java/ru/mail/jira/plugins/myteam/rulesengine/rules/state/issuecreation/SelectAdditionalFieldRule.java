/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.CreatingIssueState;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.SelectingIssueAdditionalFieldsState;

@Rule(
    name = "select additional field",
    description = "Calls when issue additional field was selected")
public class SelectAdditionalFieldRule extends BaseRule {

  static final RuleType NAME = StateActionRuleType.SelectAdditionalField;

  private final IssueCreationService issueCreationService;

  public SelectAdditionalFieldRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueCreationService issueCreationService) {
    super(userChatService, rulesEngine);
    this.issueCreationService = issueCreationService;
  }

  @Condition
  public boolean isValid(
      @Fact("prevState") BotState prevState,
      @Fact("state") BotState state,
      @Fact("command") String command) {
    return prevState instanceof CreatingIssueState
        && state instanceof SelectingIssueAdditionalFieldsState
        && NAME.equalsName(command);
  }

  @Action
  public void execute(
      @Fact("event") ButtonClickEvent event,
      @Fact("prevState") CreatingIssueState prevState,
      @Fact("args") String fieldId)
      throws MyteamServerErrorException, IOException {

    userChatService.answerCallbackQuery(event.getQueryId());

    userChatService.deleteState(event.getChatId());

    prevState.addField(issueCreationService.getField(fieldId));

    userChatService.setState(event.getChatId(), prevState);

    rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
    //    userChatService.sendMessageText(
    //        event.getChatId(), fieldId + issueCreationService.isFieldSupported(fieldId));
  }
}
