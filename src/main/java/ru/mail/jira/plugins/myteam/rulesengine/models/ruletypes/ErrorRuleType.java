/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes;

import lombok.Getter;

public enum ErrorRuleType implements RuleType {
  IssueNotFound("issueNotFound");

  @Getter(onMethod_ = {@Override})
  private final String name;

  ErrorRuleType(String name) {
    this.name = name;
  }

  @Override
  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }
}
