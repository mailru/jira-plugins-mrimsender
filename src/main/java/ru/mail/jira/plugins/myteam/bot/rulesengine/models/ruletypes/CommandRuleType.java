/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes;

import lombok.Getter;

public enum CommandRuleType implements RuleType {
  Help("help"),
  Menu("menu"),
  Issue("issue"),
  SearchByJql("jql"),
  WatchingIssues("watching"),
  AssignedIssues("assigned"),
  CreatedIssues("created"),
  AssignIssue("assign"),
  WatchIssue("watch"),
  LinkIssueWithChat("link"),
  UnwatchIssue("unwatch"),
  IssueCreationSettings("configure_task"),
  IssueTransition("transition"),
  CreateIssueByReply("createIssueByReply");

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
