/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;

public class CommentingIssueState extends BotState {
  @Getter private final String issueKey;

  public CommentingIssueState(String issueKey) {
    this.issueKey = issueKey;
  }
}