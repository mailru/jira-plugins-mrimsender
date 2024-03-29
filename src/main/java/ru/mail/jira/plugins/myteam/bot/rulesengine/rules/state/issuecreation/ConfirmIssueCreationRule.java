/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state.issuecreation;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IssueCreationValidationException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.creation.CreatingIssueState;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
@Rule(name = "confirm issue creation", description = "Creates issue by inserted fields from state")
public class ConfirmIssueCreationRule extends BaseRule {

  static final RuleType NAME = StateActionRuleType.ConfirmIssueCreation;
  private final IssueCreationService issueCreationService;

  public ConfirmIssueCreationRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueCreationService issueCreationService) {
    super(userChatService, rulesEngine);
    this.issueCreationService = issueCreationService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command, @Fact("state") BotState state) {
    return state instanceof CreatingIssueState && NAME.equalsName(command);
  }

  @Action
  public void execute(
      @Fact("event") ButtonClickEvent event, @Fact("state") CreatingIssueState state)
      throws MyteamServerErrorException, IOException {

    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getChatId());

    userChatService.answerCallbackQuery(event.getQueryId());
    try {
      Issue issue =
          issueCreationService.createIssue(
              state.getProject(), state.getIssueType(), state.getFieldValues(), user);

      if (issue != null) {
        String issueLink = messageFormatter.createIssueLink(issue.getKey());
        userChatService.sendMessageText(
            event.getChatId(),
            userChatService.getText(
                "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.issueCreated",
                issueLink));
      }
      userChatService.deleteState(event.getChatId());
    } catch (IssueCreationValidationException e) {
      log.error(e.getLocalizedMessage(), e);
      userChatService.sendMessageText(
          event.getChatId(),
          Utils.shieldText(
              String.join(
                  "\n",
                  userChatService.getRawText(
                      "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.validationError"),
                  Utils.stringifyMap(e.getErrors().getErrors()),
                  Utils.stringifyCollection(e.getErrors().getErrorMessages()))));
    }
    userChatService.deleteState(event.getChatId());
  }
}
