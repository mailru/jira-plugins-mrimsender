/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.service;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.RuleEventType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.JqlSearchState;

@Slf4j
@Rule(name = "jql search", description = "Shows issues by JQL")
public class SearchByJQLIssuesRule extends BaseRule {
  static final RuleEventType NAME = RuleEventType.SearchByJql;
  private final IssueService issueService;

  public SearchByJQLIssuesRule(UserChatService userChatService, IssueService issueService) {
    super(userChatService);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command, @Fact("args") String jql) {
    return NAME.equalsName(command) && jql != null && jql.length() > 0;
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String jql) {
    JqlSearchState newState = new JqlSearchState(userChatService, issueService, event, jql);
    try {
      newState.updateMessage(event, false);
    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage());
    }
    userChatService.setState(event.getChatId(), newState);
  }
}
