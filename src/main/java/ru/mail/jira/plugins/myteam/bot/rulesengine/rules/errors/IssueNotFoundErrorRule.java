/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.errors;

import java.io.IOException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

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
  public void execute(@Fact("event") MyteamEvent event, @Fact("exception") Exception e)
      throws MyteamServerErrorException, IOException {
    String message = userChatService.getRawText("");
    if (e.getLocalizedMessage() != null) {
      message += "\n\n" + e.getLocalizedMessage();
    }
    userChatService.sendMessageText(event.getChatId(), message);
  }
}
