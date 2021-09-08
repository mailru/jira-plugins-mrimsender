/* (C)2021 */
package ru.mail.jira.plugins.myteam.protocol;

import java.util.regex.Matcher;

public interface JiraMarkDownConverter {
  String convert(Matcher input);
}
