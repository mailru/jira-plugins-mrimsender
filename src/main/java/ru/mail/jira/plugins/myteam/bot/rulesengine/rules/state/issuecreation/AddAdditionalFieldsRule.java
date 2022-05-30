/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.issuecreation;

import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issuecreation.CreatingIssueState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issuecreation.SelectingIssueAdditionalFieldsState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

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
