/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states;

import lombok.Getter;
import lombok.Setter;

public class CreatingIssueState extends BotState {
  @Getter @Setter private String projectKey;

  public CreatingIssueState() {
    //    setWaiting(true);
  }
}
