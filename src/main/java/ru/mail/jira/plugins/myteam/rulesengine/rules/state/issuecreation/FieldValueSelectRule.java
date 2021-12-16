/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.configuration.createissue.customfields.CreateIssueFieldValueHandler;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getChatId());
    Locale locale = userChatService.getUserLocale(user);

    Optional<Field> field = state.getCurrentField();
    if (event instanceof ButtonClickEvent)
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
    if (field.isPresent()) {
      CreateIssueFieldValueHandler handler =
          issueCreationFieldsService.getFieldValueHandler(field.get());
      state.setCurrentFieldValue(handler.updateValue(state.getFieldValue(field.get()), value));
      state.nextField();
      rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, state, event);
    } else {
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.issueCreationConfirmation"),
          getIssueCreationConfirmButtons(locale));
    }
  }

  private List<List<InlineKeyboardMarkupButton>> getIssueCreationConfirmButtons(Locale locale) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttons.add(buttonsRow);

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.issueCreationConfirmButton.text"),
            "confirmIssueCreation"));

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.issueAddExtraFieldsButton.text"),
            "addExtraIssueFields"));
    return MessageFormatter.buildButtonsWithCancel(
        buttons,
        userChatService.getRawText(
            locale,
            "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text"));
  }
}
