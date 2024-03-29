/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.issuecreation;

import com.atlassian.jira.issue.fields.Field;
import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.configuration.createissue.customfields.CreateIssueFieldValueHandler;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.CreatingIssueState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.FillingIssueFieldState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.SelectingIssueAdditionalFieldsState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

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
    Field field = issueCreationService.getField(fieldId);

    prevState.addField(field);

    userChatService.revertState(event.getChatId());

    CreateIssueFieldValueHandler handler = issueCreationService.getFieldValueHandler(field);
    FillingIssueFieldState fillingFieldState =
        new FillingIssueFieldState(
            userChatService, rulesEngine, field, handler.isSearchable(), true);

    userChatService.setState(event.getChatId(), fillingFieldState);

    rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
  }
}
