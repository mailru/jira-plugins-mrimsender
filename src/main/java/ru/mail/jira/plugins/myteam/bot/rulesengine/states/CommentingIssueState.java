/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.states;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;

@SuppressWarnings({"NullAway"})
public class CommentingIssueState extends BotState {
  @Getter private final String issueKey;

  public CommentingIssueState(String issueKey) {
    this.issueKey = issueKey;
  }
}
