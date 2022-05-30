/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.buttons;

import com.atlassian.crowd.exception.UserNotFoundException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.CreatingIssueState;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.SelectingProjectState;
import ru.mail.jira.plugins.myteam.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "create issue", description = "start issue creation")
public class CreateIssueRule extends BaseRule {

  static final RuleType NAME = ButtonRuleType.CreateIssue;

  private final IssueService issueService;
  private final IssueCreationService issueCreationService;

  public CreateIssueRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueService issueService,
      IssueCreationService issueCreationService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
    this.issueCreationService = issueCreationService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event)
      throws MyteamServerErrorException, UserNotFoundException {
    userChatService.setState(
        event.getChatId(), new CreatingIssueState(userChatService, issueCreationService));

    SelectingProjectState newState = new SelectingProjectState(issueService, userChatService);

    newState.setWaiting(true);

    newState.updatePage(event, false);

    userChatService.setState(event.getChatId(), newState);

    userChatService.answerCallbackQuery(event.getQueryId());
  }
}
