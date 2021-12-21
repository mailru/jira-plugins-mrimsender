/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.buttons;

import com.atlassian.jira.user.ApplicationUser;
import java.util.Locale;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.CreatingIssueState;
import ru.mail.jira.plugins.myteam.rulesengine.states.SelectingProjectState;

@Rule(name = "create issue", description = "start issue creation")
public class CreateIssueRule extends BaseRule {

  static final ButtonRuleType NAME = ButtonRuleType.CreateIssue;

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
  public void execute(@Fact("event") ButtonClickEvent event) throws MyteamServerErrorException {
    userChatService.setState(
        event.getChatId(), new CreatingIssueState(userChatService, issueCreationService));

    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getChatId());
    if (user != null) {
      Locale locale = userChatService.getUserLocale(user);

      SelectingProjectState newState =
          new SelectingProjectState(
              issueService,
              userChatService,
              userChatService.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.selectProject.message"));
      newState.setWaiting(true);

      newState.updatePage(event, false);

      userChatService.setState(event.getChatId(), newState);
    }
    userChatService.answerCallbackQuery(event.getQueryId());
  }
}
