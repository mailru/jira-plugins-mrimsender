/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states;

import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.accessrequest.service.AccessRequestService;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
public class ReplyAccessState extends BotState implements CancelableState {
  private final UserChatService userChatService;

  public ReplyAccessState(
      AccessRequestService accessRequestService,
      UserChatService userChatService,
      RulesEngine rulesEngine) {
    this.userChatService = userChatService;
  }

  @Override
  public UserChatService getUserChatService() {
    return userChatService;
  }

  @Override
  public void cancel(MyteamEvent event) {
    userChatService.revertState(event.getChatId());
  }

  @Override
  public void onError(Exception e) {
    SentryClient.capture(e);
    log.error(e.getLocalizedMessage(), e);
  }
}
