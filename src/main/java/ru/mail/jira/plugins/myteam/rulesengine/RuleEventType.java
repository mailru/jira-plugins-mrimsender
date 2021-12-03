/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine;

public enum RuleEventType {
  DefaultMessage("defaultMessage"),
  Issue("issue");

  private final String name;

  RuleEventType(String s) {
    name = s;
  }

  //  public boolean equalsName(String otherName) {
  //    // (otherName == null) check is not needed because name.equals(null) returns false
  //    return name.equals(otherName);
  //  }
  @Override
  public String toString() {
    return this.name;
  }
}
