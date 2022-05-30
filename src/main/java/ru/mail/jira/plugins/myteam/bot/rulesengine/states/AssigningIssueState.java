/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
public class AssigningIssueState extends BotState implements CancelableState {
  @Getter private final String issueKey;
  private final UserChatService userChatService;

  public AssigningIssueState(String issueKey, UserChatService userChatService) {
    this.issueKey = issueKey;
    this.userChatService = userChatService;
    this.isWaiting = true;
  }

  @Override
  public UserChatService getUserChatService() {
    return userChatService;
  }

  @Override
  public void onError(Exception e) {
    SentryClient.capture(e);
    log.error(e.getLocalizedMessage(), e);
  }
}
