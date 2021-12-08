/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.service;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ServiceRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.JqlSearchState;

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
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String jql) {
    JqlSearchState newState = new JqlSearchState(userChatService, issueService, jql);

    if (jql == null || jql.length() == 0) { // if jql is not provided ask for input
      ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
      String chatId = event.getChatId();
      if (user != null) {
        Locale locale = userChatService.getUserLocale(user);

        try {
          userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
          userChatService.sendMessageText(
              chatId,
              userChatService.getRawText(
                  locale,
                  "ru.mail.jira.plugins.myteam.myteamEventsListener.searchByJqlClauseButton.insertJqlClause.message"),
              messageFormatter.buildButtonsWithCancel(
                  null,
                  userChatService.getRawText(
                      locale,
                      "ru.mail.jira.plugins.myteam.mrimsenderEventListener.cancelButton.text")));

          newState.setWaiting(true);
        } catch (MyteamServerErrorException | IOException e) {
          log.error(e.getLocalizedMessage());
        }
      }
    } else {
      try {
        newState.setWaiting(false);
        newState.updateMessage(event, false);
      } catch (MyteamServerErrorException | IOException e) {
        log.error(e.getLocalizedMessage());
      }
    }
    userChatService.setState(event.getChatId(), newState);
  }
}
