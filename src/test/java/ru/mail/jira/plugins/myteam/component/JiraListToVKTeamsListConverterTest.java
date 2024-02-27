/* (C)2024 */
package ru.mail.jira.plugins.myteam.component;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Function;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class JiraListToVKTeamsListConverterTest {

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
  void convertTextWithJiraNumberedListToVKList(int countRow) {
    // GIVEN
    String textInRow = "text";
    String textWithJiraNumberedList = buildList(row -> "#", countRow, textInRow);
    Pattern pattern = JiraMarkdownTextPattern.MULTILEVEL_NUMBERED_LIST_PATTERN;

    JiraListToVKTeamsListConverter jiraListToVKTeamsListConverter =
        JiraListToVKTeamsListConverter.of();

    // WHEN
    String result =
        JiraMarkdownToChatMarkdownConverter.convertToMarkdown(
            textWithJiraNumberedList, pattern, jiraListToVKTeamsListConverter);

    // THEN
    assertEquals(buildList(row -> row + ".", countRow, textInRow), result);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
  void convertTextWithJiraNumberListToVKListWithOneEmptySymbolInTextRow(int countRow) {
    // GIVEN
    String textWithOneEmptySymbolinTextRow = " ";
    String textWithJiraNumberedList =
        buildList(row -> "#", countRow, textWithOneEmptySymbolinTextRow);
    Pattern pattern = JiraMarkdownTextPattern.MULTILEVEL_NUMBERED_LIST_PATTERN;

    JiraListToVKTeamsListConverter jiraListToVKTeamsListConverter =
        JiraListToVKTeamsListConverter.of();

    // WHEN
    String result =
        JiraMarkdownToChatMarkdownConverter.convertToMarkdown(
            textWithJiraNumberedList, pattern, jiraListToVKTeamsListConverter);

    // THEN
    assertEquals(buildList(row -> row + ".", countRow, textWithOneEmptySymbolinTextRow), result);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
  void convertTextWithJiraUnorderedListToVKList(int countRow) {
    // GIVEN
    String textInRow = "text";
    String textWithJiraNumberedList = buildList(row -> "*", countRow, textInRow);
    Pattern pattern = JiraMarkdownTextPattern.MULTILEVEL_NUMBERED_LIST_PATTERN;

    JiraListToVKTeamsListConverter jiraListToVKTeamsListConverter =
        JiraListToVKTeamsListConverter.of();

    // WHEN
    String result =
        JiraMarkdownToChatMarkdownConverter.convertToMarkdown(
            textWithJiraNumberedList, pattern, jiraListToVKTeamsListConverter);

    // THEN
    assertEquals(buildList(row -> "±-", countRow, textInRow), result);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
  void convertTextWithJiraUnorderedListToVKListWithOneEmptySymbolInTextRow(int countRow) {
    // GIVEN
    String textWithOneEmptySymbolinTextRow = " ";
    String textWithJiraNumberedList =
        buildList(row -> "*", countRow, textWithOneEmptySymbolinTextRow);
    Pattern pattern = JiraMarkdownTextPattern.MULTILEVEL_NUMBERED_LIST_PATTERN;

    JiraListToVKTeamsListConverter jiraListToVKTeamsListConverter =
        JiraListToVKTeamsListConverter.of();

    // WHEN
    String result =
        JiraMarkdownToChatMarkdownConverter.convertToMarkdown(
            textWithJiraNumberedList, pattern, jiraListToVKTeamsListConverter);

    // THEN
    assertEquals(buildList(row -> "±-", countRow, textWithOneEmptySymbolinTextRow), result);
  }

  private static String buildList(
      Function<Integer, String> markerProvider, int countRow, String textInRow) {
    StringBuilder textWithJiraList = new StringBuilder();
    for (int i = 1; i <= countRow; i++) {
      textWithJiraList
          .append(markerProvider.apply(i))
          .append(" ")
          .append(textInRow)
          .append(countRow);
      textWithJiraList.append("\n");
    }
    return textWithJiraList.toString();
  }
}
