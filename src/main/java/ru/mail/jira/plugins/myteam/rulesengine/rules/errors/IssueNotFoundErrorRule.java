/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.errors;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Slf4j
@Rule(name = "issue not found", description = "Shows issue not found error message")
public class IssueNotFoundErrorRule extends BaseRule {
  static final RuleType NAME = ErrorRuleType.IssueNotFound;

  public IssueNotFoundErrorRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("error") ErrorRuleType error) {
    return NAME.equals(error);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("message") String message) {
    log.error(message);
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (user != null) {
      try {
        userChatService.sendMessageText(
            event.getChatId(),
            userChatService.getRawText(
                userChatService.getUserLocale(user),
                "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.issueNotFound"));
      } catch (MyteamServerErrorException | IOException e) {
        log.error(e.getLocalizedMessage());
      }
    }
  }
}
