/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.rules;

import com.atlassian.crowd.exception.UserNotFoundException;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
public class ChatAdminRule extends BaseRule {
  public ChatAdminRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  public void checkAdminRules(MyteamEvent event) throws AdminRulesRequiredException {
    if (!userChatService.isChatAdmin(event.getChatId(), event.getUserId())) {
      try {
        userChatService.sendMessageText(
            event.getUserId(),
            userChatService.getText(
                userChatService.getUserLocale(event.getUserId()),
                "ru.mail.jira.plugins.myteam.chat.adminPermissionsRequired",
                userChatService.getGroupChatName(event.getChatId())));
      } catch (MyteamServerErrorException | IOException | UserNotFoundException e) {
        log.error(e.getLocalizedMessage(), e);
      }
      throw new AdminRulesRequiredException();
    }
  }
}
