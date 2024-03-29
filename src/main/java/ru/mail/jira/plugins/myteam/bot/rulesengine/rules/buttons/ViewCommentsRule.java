/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.buttons;

import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.ViewingIssueCommentsState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.JiraMarkdownToChatMarkdownConverter;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "view issue comments", description = "View issue comments by issue key")
public class ViewCommentsRule extends BaseRule {

  static final RuleType NAME = ButtonRuleType.ViewComments;
  private final IssueService issueService;
  private final JiraMarkdownToChatMarkdownConverter jiraMarkdownToChatMarkdownConverter;

  public ViewCommentsRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueService issueService,
      JiraMarkdownToChatMarkdownConverter jiraMarkdownToChatMarkdownConverter) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
    this.jiraMarkdownToChatMarkdownConverter = jiraMarkdownToChatMarkdownConverter;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event, @Fact("args") String issueKey)
      throws MyteamServerErrorException {
    ViewingIssueCommentsState newState =
        new ViewingIssueCommentsState(
            issueKey,
            issueService,
            userChatService,
            rulesEngine,
            jiraMarkdownToChatMarkdownConverter);
    userChatService.setState(event.getChatId(), newState);
    newState.updatePage(event, false);
  }
}
