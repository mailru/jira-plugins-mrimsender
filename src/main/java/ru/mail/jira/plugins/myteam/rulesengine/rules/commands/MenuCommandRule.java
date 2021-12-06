/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.commands;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.RuleEventType;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Rule(name = "/menu", description = "Shows menu")
public class MenuCommandRule extends BaseRule {
  static final RuleEventType NAME = RuleEventType.Menu;

  public MenuCommandRule(UserChatService userChatService) {
    super(userChatService);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (user != null) {
      Locale locale = userChatService.getUserLocale(user);
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              locale, "ru.mail.jira.plugins.myteam.messageQueueProcessor.mainMenu.text"),
          messageFormatter.getMenuButtons(user));
    }
  }
}
