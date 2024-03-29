/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.issuecreation;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IncorrectIssueTypeException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.UnsupportedCustomFieldsException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.CreatingIssueState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.FillingIssueFieldState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
@Rule(name = "select issue type", description = "Selects issue type while creating new issue")
public class IssueTypeSelectButtonRule extends BaseRule {

  static final RuleType NAME = StateActionRuleType.SelectIssueType;
  private final IssueService issueService;

  public IssueTypeSelectButtonRule(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command, @Fact("state") BotState state) {
    return state instanceof CreatingIssueState && NAME.equalsName(command);
  }

  @Action
  public void execute(
      @Fact("event") ButtonClickEvent event,
      @Fact("state") CreatingIssueState state,
      @Fact("args") String issueTypeId)
      throws MyteamServerErrorException, IOException, UserNotFoundException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    String chatId = event.getChatId();
    userChatService.answerCallbackQuery(event.getQueryId());

    try {
      state.setIssueType(
          issueService.getIssueType(issueTypeId),
          user); // set issue type and load required fields meta

      Optional<Field> lastField = state.getCurrentField();

      if (lastField.isPresent()) {
        FillingIssueFieldState fillingFieldState =
            new FillingIssueFieldState(userChatService, rulesEngine, lastField.get(), false, false);
        userChatService.setState(event.getChatId(), fillingFieldState);
      }

      rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);

    } catch (UnsupportedCustomFieldsException e) {
      log.error(e.getLocalizedMessage(), e);
      userChatService.sendMessageText(
          chatId,
          String.join(
              "\n",
              userChatService.getRawText(
                  "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.requiredCFError"),
              messageFormatter.stringifyFieldsCollection(e.getRequiredCustomFields())));

    } catch (IncorrectIssueTypeException e) {
      log.error(e.getLocalizedMessage(), e);
      userChatService.sendMessageText(
          chatId,
          userChatService.getRawText(
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectedIssueTypeNotValid"));
    }
  }
}
