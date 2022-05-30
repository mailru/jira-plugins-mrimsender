/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes;

public interface RuleType {

  String getName();

  boolean equalsName(String otherName);
}
