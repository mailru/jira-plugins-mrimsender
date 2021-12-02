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
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.UserChatService;

@Rule(name = "/help command rule", description = "shows help")
public class HelpCommandRule extends BaseCommandRule {
  static final String NAME = "help";

  public HelpCommandRule(UserChatService userChatService) {
    super(userChatService);
  }

  @Condition
  public boolean isHelpCommand(@Fact("command") String command) {
    return command.equals(NAME);
  }

  @Action
  public void showHelp(@Fact("event") ChatMessageEvent event)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());

    Locale locale = userChatService.getUserLocale(user);
    if (event.getChatType() == ChatType.GROUP)
      myteamClient.sendMessageText(
          event.getChatId(),
          i18nResolver.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.helpMessage.text"));
    else
      myteamClient.sendMessageText(
          event.getChatId(),
          i18nResolver.getRawText(
              locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.helpMessage.text"));
  }
}
