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
import ru.mail.jira.plugins.myteam.rulesengine.models.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ServiceRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.ViewingJqlSearchResultsState;

@Slf4j
@Rule(name = "jql search", description = "Shows issues by JQL")
public class SearchByJqlIssuesRule extends BaseRule {
  static final RuleType NAME = ServiceRuleType.SearchByJql;
  private final IssueService issueService;

  public SearchByJqlIssuesRule(UserChatService userChatService, IssueService issueService) {
    super(userChatService);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command, @Fact("args") String jql) {
    return NAME.equalsName(command) && jql != null && jql.length() > 0;
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String jql) {
    ViewingJqlSearchResultsState newState =
        new ViewingJqlSearchResultsState(userChatService, issueService, event, jql);
    try {
      newState.updateMessage(event, false);
    } catch (MyteamServerErrorException | IOException e) {
      log.error(e.getLocalizedMessage());
    }
    userChatService.setState(event.getChatId(), newState);
  }
}
