/* (C)2022 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states.issue.transition;

import com.atlassian.jira.issue.Issue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@SuppressWarnings({"NullAway"})
@Slf4j
public class IssueTransitionEditingState extends BotState implements CancelableState {
  @Getter private final Issue issue;
  private final UserChatService userChatService;

  public IssueTransitionEditingState(Issue issue, UserChatService userChatService) {
    this.issue = issue;
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
