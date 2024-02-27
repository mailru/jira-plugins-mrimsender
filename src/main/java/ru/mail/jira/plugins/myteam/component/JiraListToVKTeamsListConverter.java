/* (C)2024 */
package ru.mail.jira.plugins.myteam.component;

import java.util.function.Function;
import java.util.regex.Matcher;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class JiraListToVKTeamsListConverter implements Function<Matcher, String> {
  private int orderedListCount = 1;

  private JiraListToVKTeamsListConverter() {}

  public static JiraListToVKTeamsListConverter of() {
    return new JiraListToVKTeamsListConverter();
  }

  @Override
  public String apply(Matcher matcher) {
    if (matcher.group(1).equals("*")) {
      return "±- " + matcher.group(2);
    } else {
      return (String.format("%s. ", orderedListCount++) + matcher.group(2));
    }
  }
}
