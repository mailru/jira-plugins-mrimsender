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
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issuecreation.FillingIssueFieldState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.commons.exceptions.ValidationException;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "edit issue creation value", description = "Calls when issue field value was edited")
public class FieldValueEditRule extends BaseRule {

  static final RuleType NAME = StateActionRuleType.EditIssueCreationValue;

  private final IssueCreationService issueCreationService;

  public FieldValueEditRule(
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

    try {
      state.setValue(handler.updateValue(state.getValue(), value, event));
    } catch (ValidationException e) {
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getText(
              userChatService.getUserLocale(event.getChatId()),
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.insertIssueField.validationError",
              e.getLocalizedMessage()));
    } finally {
      rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
    }
  }
}
