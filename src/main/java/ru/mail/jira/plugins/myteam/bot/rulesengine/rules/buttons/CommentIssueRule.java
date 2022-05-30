/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.buttons;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.CommentingIssueState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "comment issue", description = "start waiting for comment input")
public class CommentIssueRule extends BaseRule {

  static final RuleType NAME = ButtonRuleType.CommentIssue;

  public CommentIssueRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event, @Fact("args") String issueKey)
      throws MyteamServerErrorException, IOException {
    CommentingIssueState newState = new CommentingIssueState(issueKey);
    newState.setWaiting(true);
    userChatService.setState(event.getChatId(), newState);
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());

    Locale locale = userChatService.getUserLocale(user);
    String message =
        userChatService.getText(
            locale,
            "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.insertComment.message",
            issueKey);

    userChatService.sendMessageText(
        event.getChatId(), message, messageFormatter.getCancelButton(locale));
    userChatService.answerCallbackQuery(event.getQueryId());
  }
}
