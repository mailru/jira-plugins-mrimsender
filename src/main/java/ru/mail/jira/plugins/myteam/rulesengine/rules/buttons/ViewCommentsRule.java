/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.buttons;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.ViewingIssueCommentsState;

@Slf4j
@Rule(name = "view issue comments", description = "View issue comments by issue key")
public class ViewCommentsRule extends BaseRule {

  static final ButtonRuleType NAME = ButtonRuleType.ViewComments;
  private final IssueService issueService;

  public ViewCommentsRule(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event, @Fact("args") String issueKey)
      throws MyteamServerErrorException {
    ViewingIssueCommentsState newState =
        new ViewingIssueCommentsState(issueKey, issueService, userChatService, rulesEngine);
    try {
      userChatService.setState(event.getChatId(), newState);
      newState.updateMessage(event, false);
    } catch (IOException e) {
      log.error(e.getLocalizedMessage());
    }
  }
}
