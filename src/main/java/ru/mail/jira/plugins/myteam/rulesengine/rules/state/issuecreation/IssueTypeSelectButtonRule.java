/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecreation;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IncorrectIssueTypeException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.UnsupportedCustomFieldsException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.CreatingIssueState;

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

    Locale locale = userChatService.getUserLocale(user);
    try {
      state.setIssueType(issueService.getIssueType(issueTypeId), user);
      rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
    } catch (UnsupportedCustomFieldsException e) {
      log.error(e.getLocalizedMessage());
      userChatService.sendMessageText(
          chatId,
          String.join(
              "\n",
              userChatService.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.requiredCFError"),
              messageFormatter.stringifyFieldsCollection(locale, e.getRequiredCustomFields())));

    } catch (IncorrectIssueTypeException e) {
      log.error(e.getLocalizedMessage());
      userChatService.sendMessageText(
          chatId,
          userChatService.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectedIssueTypeNotValid"));
    }
  }
}
