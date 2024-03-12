/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.issuecreation;

import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.configuration.createissue.FieldInputMessageInfo;
import ru.mail.jira.plugins.myteam.bot.configuration.createissue.customfields.CreateIssueFieldValueHandler;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.CreatingIssueState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.FillingIssueFieldState;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

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
    return (state instanceof CreatingIssueState || state instanceof FillingIssueFieldState)
        && NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("state") BotState state)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    String chatId = event.getChatId();

    BotState prevState = userChatService.getPrevState(event.getChatId());

    CreatingIssueState issueCreationState =
        (CreatingIssueState) (state instanceof CreatingIssueState ? state : prevState);

    if (issueCreationState == null)
      throw new MyteamServerErrorException(500, "Issue Creation Bot State is empty.");

    Optional<Field> field = issueCreationState.getCurrentField();

    if (!field.isPresent()) {
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
                  "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.issueCreationConfirmation")
              + issueCreationState.createInsertFieldMessage(""),
          getIssueCreationConfirmButtons());

    } else {
      CreateIssueFieldValueHandler handler = issueCreationService.getFieldValueHandler(field.get());

      FillingIssueFieldState fillingFieldState = null;

      if (state instanceof FillingIssueFieldState) {
        fillingFieldState = (FillingIssueFieldState) state;
      }

      if (fillingFieldState == null) {
        return;
      }

      FieldInputMessageInfo msgInfo =
          handler.getMessageInfo(
              issueCreationState.getProject(),
              issueCreationState.getIssueType(),
              user,
              fillingFieldState);

      String msg = issueCreationState.createInsertFieldMessage(msgInfo.getMessage());

      List<List<InlineKeyboardMarkupButton>> buttons =
          fillingFieldState.isAdditionalField()
              ? MessageFormatter.buildButtonsWithBack(
                  msgInfo.getButtons(),
                  userChatService.getRawText(
                      "ru.mail.jira.plugins.myteam.mrimsenderEventListener.cancelButton.text"))
              : MessageFormatter.buildButtonsWithCancel(
                  msgInfo.getButtons(),
                  userChatService.getRawText(
                      "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text"));

      final String shieldMsg = Utils.shieldText(msg);
      if (event instanceof ButtonClickEvent) {
        userChatService.editMessageText(
            chatId,
            ((ButtonClickEvent) event).getMsgId(),
            shieldMsg != null ? shieldMsg : msg,
            buttons);
      } else {
        userChatService.sendMessageText(
            event.getChatId(), shieldMsg != null ? shieldMsg : msg, buttons);
      }
    }

    if (event instanceof ButtonClickEvent) {
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
    }
  }

  private List<List<InlineKeyboardMarkupButton>> getIssueCreationConfirmButtons() {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttons.add(buttonsRow);

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.issueCreationConfirmButton.text"),
            StateActionRuleType.ConfirmIssueCreation.getName()));

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            userChatService.getRawText(
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.issueAddExtraFieldsButton.text"),
            StateActionRuleType.AddAdditionalFields.getName()));
    return MessageFormatter.buildButtonsWithCancel(
        buttons,
        userChatService.getRawText(
            "ru.mail.jira.plugins.myteam.myteamEventsListener.cancelIssueCreationButton.text"));
  }
}
