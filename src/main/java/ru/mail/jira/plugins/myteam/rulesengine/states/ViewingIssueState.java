/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
public class ViewingIssueState extends BotState implements CancelableState {
  @Getter @Setter private String issueKey;
  private final UserChatService userChatService;

  public ViewingIssueState(UserChatService userChatService, String issueKey) {
    this.issueKey = issueKey;
    this.userChatService = userChatService;
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
