/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models;

import lombok.Getter;

public enum ButtonRuleType implements RuleType {
  NextPage("next"),
  PrevPage("previous");

  @Getter(onMethod_ = {@Override})
  private final String name;

  ButtonRuleType(String name) {
    this.name = name;
  }

  @Override
  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }
}
