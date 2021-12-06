/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models;

import lombok.Getter;

public enum RuleEventType {
  DefaultMessage("defaultMessage"),
  Help("help"),
  Menu("menu"),
  Issue("issue"),
  WatchingIssues("watching"),
  SearchByJql("jql"),
  NextPage("next"),
  PrevPage("previous");

  @Getter private final String name;

  RuleEventType(String name) {
    this.name = name;
  }

  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }
}
