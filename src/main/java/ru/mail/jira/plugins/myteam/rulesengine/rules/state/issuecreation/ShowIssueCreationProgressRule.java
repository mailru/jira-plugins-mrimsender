/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.configuration.createissue.customfields.CreateIssueFieldValueHandler;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.CreatingIssueState;

@Rule(
    name = "show issue creation message",
    description = "Calls to show filled and current filling fields")
public class ShowIssueCreationProgressRule extends BaseRule {

  static final RuleType NAME = StateActionRuleType.ShowCreatingIssueProgressMessage;

  private final IssueCreationService issueCreationService;

  public ShowIssueCreationProgressRule(
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
      @Fact("args") String messagePrefix)
      throws MyteamServerErrorException, IOException, UserNotFoundException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    String chatId = event.getChatId();
    Locale locale = userChatService.getUserLocale(user);

    Optional<Field> field = state.getCurrentField();

    if (field.isPresent()) {
      CreateIssueFieldValueHandler handler = issueCreationService.getFieldValueHandler(field.get());
      userChatService.sendMessageText(
          chatId,
          state.createInsertFieldMessage(
              locale,
              messagePrefix == null || messagePrefix.length() == 0
                  ? handler.getInsertFieldMessage(field.get(), locale)
                  : messagePrefix),
          MessageFormatter.buildButtonsWithCancel(
              handler.getButtons(
                  field.get(),
                  state.getFieldValue(field.get()),
                  userChatService.getUserLocale(user)),
              userChatService.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text")));
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
            StateActionRuleType.ConfirmIssueCreation.getName()));

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.issueAddExtraFieldsButton.text"),
            StateActionRuleType.AddAdditionalFields.getName()));
    return MessageFormatter.buildButtonsWithCancel(
        buttons,
        userChatService.getRawText(
            locale,
            "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text"));
  }
}
