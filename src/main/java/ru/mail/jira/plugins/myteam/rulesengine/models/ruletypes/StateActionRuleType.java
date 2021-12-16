/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes;

import lombok.Getter;

public enum StateActionRuleType implements RuleType {
  ShowCreatingIssueProgressMessage("showCreatingIssueProgressMessage");

  @Getter(onMethod_ = {@Override})
  private final String name;

  StateActionRuleType(String name) {
    this.name = name;
  }

  @Override
  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }
}
