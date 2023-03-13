/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes;

import java.util.List;

public interface RuleType {

  String DELIMITER = "&&&";

  String getName();

  default boolean equalsName(String otherName) {
    return getName().equals(otherName);
  }

  static List<String> parseArgs(String args) {
    return List.of(args.split(DELIMITER));
  }

  static String joinArgs(List<String> args) {
    return String.join(DELIMITER, args);
  }
}
