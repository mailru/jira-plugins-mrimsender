/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import com.atlassian.diff.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class DiffFieldChatMessageGenerator {

  private static final int MAX_WHITESPACE_PRESERVATION_LENGTH = 20;

  public String markOldValue(@NotNull final String oldValue) {
    if (StringUtils.isBlank(oldValue)) {
      return "";
    }
    return "~" + oldValue + "~";
  }

  public String markNewValue(@NotNull final String newValue) {
    if (StringUtils.isBlank(newValue)) {
      return "";
    }
    return "*" + newValue + "*";
  }

  /**
   * Building diff between two strings
   *
   * @see com.atlassian.jira.web.action.util.DiffViewRenderer#getUnifiedHtml(DiffViewBean, String,
   *     String) reused method for show diff in text
   * @param firstString original string
   * @param secondString new string
   * @return diff in lines
   */
  @NotNull
  public String buildDiffString(
      @NotNull final String firstString, @NotNull final String secondString) {
    final DiffViewBean diffViewBean = DiffViewBean.createWordLevelDiff(firstString, secondString);
    final StringBuilder diffBuilder = new StringBuilder();
    final String removedStyle = "~";
    final String addedStyle = "*";
    for (DiffChunk chunk : diffViewBean.getUnifiedChunks()) {
      if (chunk.getType() == DiffType.CHANGED_WORDS) {
        WordChunk wordChunk = (WordChunk) chunk;
        for (CharacterChunk charChunk : wordChunk.getCharacterChunks()) {

          if (charChunk.getType() == DiffType.DELETED_CHARACTERS) {
            diffBuilder.append(removedStyle);
          } else if (charChunk.getType() == DiffType.ADDED_CHARACTERS) {
            diffBuilder.append(addedStyle);
          }
          diffBuilder.append(print(charChunk.getText()));
          if (charChunk.getType() == DiffType.DELETED_CHARACTERS) {
            diffBuilder.append(removedStyle);
          } else if (charChunk.getType() == DiffType.ADDED_CHARACTERS) {
            diffBuilder.append(addedStyle);
          }
        }
      } else if (chunk
          .getType()
          .getClassName()
          .equals("unchanged")) // probably dead code, but copied from old line-diff.vm
      {
        diffBuilder.append(print(chunk.getText()));
      } else {

        if (chunk.getType() == DiffType.DELETED_WORDS) {
          diffBuilder.append(removedStyle);
        } else if (chunk.getType() == DiffType.ADDED_WORDS) {
          diffBuilder.append(addedStyle);
        }
        diffBuilder.append(print(chunk.getText()));
        if (chunk.getType() == DiffType.DELETED_WORDS) {
          diffBuilder.append(removedStyle);
        } else if (chunk.getType() == DiffType.ADDED_WORDS) {
          diffBuilder.append(addedStyle);
        }
      }
      diffBuilder.append(" "); // ensure visual spacing
    }

    return diffBuilder.toString().replaceAll("<br>", "\n").replaceAll("&nbsp;", "");
  }

  /**
   * Reused method from JIRA API
   *
   * @see com.atlassian.jira.web.action.util.DiffViewRenderer#print(String) reused method for
   *     converting JIRA markup to VKTeams markup
   * @param string some string to build general diff string
   * @return input string without newline chars
   */
  private static String print(String string) {
    // 1. replace new line characters with a line break html character
    string = string.replaceAll("(\\r\\n|\\n|\\r)", "<br>");

    // 2. preserve whitespace blocks up greater than 2 up to a threshold
    // (MAX_WHITESPACE_PRESERVATION_LENGTH)
    final StringBuffer result = new StringBuffer();
    final Matcher matcher = Pattern.compile("(\\s{2,})").matcher(string);

    while (matcher.find()) {
      String match = matcher.group(0);
      int length = match.length();

      // replace up to MAX_WHITESPACE_PRESERVATION_LENGTH with "&nbsp;" and then the rest are normal
      // spaces
      String replacement =
          StringUtils.repeat("&nbsp;", Math.min(length, MAX_WHITESPACE_PRESERVATION_LENGTH))
              + StringUtils.repeat(" ", Math.max(0, length - MAX_WHITESPACE_PRESERVATION_LENGTH));

      // replace the result while maintaining correct index's for the next replacement
      matcher.appendReplacement(result, replacement);
    }

    return matcher.appendTail(result).toString();
  }
}
