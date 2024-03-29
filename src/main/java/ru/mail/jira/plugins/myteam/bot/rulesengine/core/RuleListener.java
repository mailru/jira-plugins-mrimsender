/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.core;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
public class RuleListener implements org.jeasy.rules.api.RuleListener {
  private final UserChatService userChatService;

  public RuleListener(UserChatService userChatService) {
    this.userChatService = userChatService;
  }

  @Override
  public void onEvaluationError(Rule rule, Facts facts, Exception exception) {
    if (checkAdminRulesException(facts, exception)) return;

    SentryClient.capture(exception);
    log.error(
        "Rule evaluation {} was failed with facts: {}\nMessage: {}",
        rule.getName(),
        facts.toString(),
        exception.getLocalizedMessage(),
        exception);
  }

  @Override
  public void onFailure(Rule rule, Facts facts, Exception exception) {
    if (checkAdminRulesException(facts, exception)) return;

    SentryClient.capture(exception);
    log.error(
        "Rule {} was failed with facts: {}\nMessage: {}",
        rule.getName(),
        facts.toString(),
        exception.getLocalizedMessage(),
        exception);
  }

  private boolean checkAdminRulesException(Facts facts, Exception exception) {
    if (exception instanceof UndeclaredThrowableException
        && ((UndeclaredThrowableException) exception).getUndeclaredThrowable().getCause()
            instanceof AdminRulesRequiredException) {
      MyteamEvent event = facts.get("event");
      if (event != null) {
        try {
          userChatService.sendMessageText(
              event.getUserId(),
              userChatService.getText(
                  "ru.mail.jira.plugins.myteam.chat.adminPermissionsRequired",
                  userChatService.getGroupChatName(event.getChatId())));
        } catch (MyteamServerErrorException | IOException e) {
          log.error(e.getLocalizedMessage(), e);
        }
      }
      return true;
    }
    return false;
  }
}
