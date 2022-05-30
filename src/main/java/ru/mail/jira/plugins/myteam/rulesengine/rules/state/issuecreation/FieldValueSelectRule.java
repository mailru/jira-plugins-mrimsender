/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Optional;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.createissue.customfields.CreateIssueFieldValueHandler;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.commons.exceptions.ValidationException;
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
      throws MyteamServerErrorException, IOException, UserNotFoundException {
    Field field = state.getField();
    if (event instanceof ButtonClickEvent) {
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
    }

    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getChatId());

    CreateIssueFieldValueHandler handler = issueCreationService.getFieldValueHandler(field);

    BotState prevState = userChatService.getPrevState(event.getChatId());

    userChatService.revertState(event.getChatId());

    if (prevState instanceof CreatingIssueState) {
      try {
        ((CreatingIssueState) prevState)
            .setFieldValue(field, handler.updateValue(state.getValue(), value, event));
      } catch (ValidationException e) {
        userChatService.sendMessageText(
            event.getChatId(),
            userChatService.getText(
                userChatService.getUserLocale(user),
                "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.validationError",
                e.getLocalizedMessage()));
        rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
      }

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
