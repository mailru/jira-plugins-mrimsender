/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes;

import lombok.Getter;

public enum CommandRuleType implements RuleType {
  Help("help"),
  Menu("menu"),
  Issue("issue"),
  SearchByJql("jql"),
  WatchingIssues("watching"),
  AssignedIssues("assigned"),
  CreatedIssues("created"),
  WatchIssue("watch"),
  UnwatchIssue("unwatch");

  @Getter(onMethod_ = {@Override})
  private final String name;

  CommandRuleType(String name) {
    this.name = name;
  }

  @Override
  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }
}
