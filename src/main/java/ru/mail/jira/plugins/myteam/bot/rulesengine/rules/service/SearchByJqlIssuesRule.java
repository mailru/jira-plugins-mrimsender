/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service;

import java.io.IOException;
import java.util.Locale;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.JqlSearchState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "jql search", description = "Shows issues by JQL")
public class SearchByJqlIssuesRule extends BaseRule {
  static final RuleType NAME = CommandRuleType.SearchByJql;
  private final IssueService issueService;

  public SearchByJqlIssuesRule(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String jql)
      throws MyteamServerErrorException, IOException {
    JqlSearchState newState = new JqlSearchState(userChatService, issueService, jql);

    if (jql == null || jql.length() == 0) { // if jql is not provided ask for input
      String chatId = event.getChatId();
      userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
      userChatService.sendMessageText(
          chatId,
          userChatService.getRawText(
              "ru.mail.jira.plugins.myteam.myteamEventsListener.searchByJqlClauseButton.insertJqlClause.message"),
          MessageFormatter.buildButtonsWithCancel(
              null,
              userChatService.getRawText(
                  "ru.mail.jira.plugins.myteam.mrimsenderEventListener.cancelButton.text")));

      newState.setWaiting(true);

    } else {
      newState.setWaiting(false);
      newState.updatePage(event, false);
    }
    userChatService.setState(event.getChatId(), newState);
  }
}
