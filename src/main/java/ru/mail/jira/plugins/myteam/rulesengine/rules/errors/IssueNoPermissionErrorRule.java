/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.errors;

import com.atlassian.crowd.exception.UserNotFoundException;
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
@Rule(name = "no issue permissions", description = "Shows no permission for issue error message")
public class IssueNoPermissionErrorRule extends BaseRule {
  static final RuleType NAME = ErrorRuleType.IssueNoPermission;

  public IssueNoPermissionErrorRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("error") ErrorRuleType error) {
    return NAME.equals(error);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("exception") Exception e)
      throws UserNotFoundException, MyteamServerErrorException, IOException {
    log.error(e.getLocalizedMessage(), e);

    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());

    userChatService.sendMessageText(
        event.getChatId(),
        userChatService.getRawText(
            userChatService.getUserLocale(user),
            "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
  }
}
