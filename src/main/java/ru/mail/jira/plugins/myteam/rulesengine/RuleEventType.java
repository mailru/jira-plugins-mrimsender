/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine;

public enum RuleEventType {
  DefaultMessage("defaultMessage"),
  Help("help"),
  Menu("menu"),
  Issue("issue");

  private final String name;

  RuleEventType(String s) {
    name = s;
  }

  public boolean equalsName(String otherName) {
    return name.equals(otherName);
  }

  @Override
  public String toString() {
    return this.name;
  }
}
