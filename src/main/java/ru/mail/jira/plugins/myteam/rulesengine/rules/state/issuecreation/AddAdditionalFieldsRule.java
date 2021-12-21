/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
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
    name = "additional fields",
    description = "Calls when issue additional field button was clicked")
public class AddAdditionalFieldsRule extends BaseRule {

  static final RuleType NAME = StateActionRuleType.AddAdditionalFields;

  private final IssueCreationService issueCreationService;

  public AddAdditionalFieldsRule(
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
      @Fact("event") ButtonClickEvent event, @Fact("state") CreatingIssueState state)
      throws MyteamServerErrorException, IOException {
    SelectingIssueAdditionalFieldsState newState =
        new SelectingIssueAdditionalFieldsState(
            state.getProject(),
            state.getIssueType(),
            userChatService,
            issueCreationService,
            rulesEngine);

    userChatService.answerCallbackQuery(event.getQueryId());

    newState.updatePage(event, false);

    userChatService.setState(event.getChatId(), newState);
  }
}
