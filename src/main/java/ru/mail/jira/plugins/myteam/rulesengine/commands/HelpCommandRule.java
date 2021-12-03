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
import ru.mail.jira.plugins.myteam.rulesengine.RuleEventType;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Rule(name = "/help command rule", description = "shows help")
public class HelpCommandRule extends BaseCommandRule {

  static final RuleEventType NAME = RuleEventType.Help;

  public HelpCommandRule(UserChatService userChatService) {
    super(userChatService);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());

    Locale locale = userChatService.getUserLocale(user);
    if (event.getChatType() == ChatType.GROUP)
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.helpMessage.text"));
    else
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.helpMessage.text"));
  }
}
