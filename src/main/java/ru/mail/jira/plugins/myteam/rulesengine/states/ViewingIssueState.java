/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;

@NoArgsConstructor
public class ViewingIssueState extends BotState {
  @Getter @Setter private String issueKey;

  public ViewingIssueState(String issueKey) {
    this.issueKey = issueKey;
  }
}
