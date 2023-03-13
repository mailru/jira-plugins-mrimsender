/* (C)2023 */
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

@Rule(name = "unknown error", description = "Shows unknown error message")
public class UnknownErrorRule extends BaseRule {
  static final RuleType NAME = ErrorRuleType.UnknownError;

  public UnknownErrorRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("error") ErrorRuleType error) {
    return NAME.equals(error);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event, @Fact("exception") Exception e)
      throws MyteamServerErrorException, IOException {
    userChatService.sendMessageText(
        event.getChatId(),
        userChatService.getRawText(
                "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.unknownError")
            + "\n\n"
            + e.getLocalizedMessage());
  }
}
