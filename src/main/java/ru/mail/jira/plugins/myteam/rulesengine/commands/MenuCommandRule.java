/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.commands;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Rule(name = "/menu command rule", description = "shows menu")
public class MenuCommandRule extends BaseCommandRule {
  static final String NAME = "menu";

  public MenuCommandRule(UserChatService userChatService) {
    super(userChatService);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return command.equals(NAME);
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (user != null) {
      Locale locale = userChatService.getUserLocale(user);
      myteamClient.sendMessageText(
          event.getChatId(),
          i18nResolver.getRawText(
              locale, "ru.mail.jira.plugins.myteam.messageQueueProcessor.mainMenu.text"),
          messageFormatter.getMenuButtons(user));
    }
  }
}
