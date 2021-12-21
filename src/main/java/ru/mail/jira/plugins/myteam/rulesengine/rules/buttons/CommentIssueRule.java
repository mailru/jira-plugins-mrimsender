/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.buttons;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.CommentingIssueState;

@Slf4j
@Rule(name = "comment issue", description = "start waiting for comment input")
public class CommentIssueRule extends BaseRule {

  static final ButtonRuleType NAME = ButtonRuleType.CommentIssue;

  public CommentIssueRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event, @Fact("args") String issueKey)
      throws MyteamServerErrorException {
    CommentingIssueState newState = new CommentingIssueState(issueKey);
    newState.setWaiting(true);
    try {
      userChatService.setState(event.getChatId(), newState);
      ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getChatId());
      if (user != null) {
        Locale locale = userChatService.getUserLocale(user);
        String message =
            userChatService.getText(
                locale,
                "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.insertComment.message",
                issueKey);

        userChatService.sendMessageText(
            event.getChatId(), message, messageFormatter.getCancelButton(locale));
      }
    } catch (IOException e) {
      log.error(e.getLocalizedMessage());
    }
    userChatService.answerCallbackQuery(event.getQueryId());
  }
}
