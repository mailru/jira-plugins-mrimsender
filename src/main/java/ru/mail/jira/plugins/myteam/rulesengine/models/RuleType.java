/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.models;

public interface RuleType {

  String getName();

  boolean equalsName(String otherName);
}
