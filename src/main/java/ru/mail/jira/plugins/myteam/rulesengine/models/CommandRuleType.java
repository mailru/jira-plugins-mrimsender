/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models;

import lombok.Getter;

public enum CommandRuleType implements RuleType {
  Help("help"),
  Menu("menu"),
  Issue("issue"),
  WatchingIssues("watching"),
  AssignedIssues("assigned"),
  CreatedIssues("created");

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
