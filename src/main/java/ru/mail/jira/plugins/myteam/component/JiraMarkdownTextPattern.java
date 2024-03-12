/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import java.util.regex.Pattern;

public final class JiraMarkdownTextPattern {

  static final Pattern CODE_BLOCK_PATTERN_1 =
      Pattern.compile("\\{[Cc]ode:([a-z]+?)}([^+]*?)\\{[Cc]ode}", Pattern.MULTILINE);
  static final Pattern CODE_BLOCK_PATTERN_2 =
      Pattern.compile("\\{[Cc]ode}([^+]*?)\\{[Cc]ode}", Pattern.MULTILINE);
  static final Pattern INLINE_CODE_PATTERN = Pattern.compile("\\{\\{([^}?\\n]+)}}");
  static final Pattern QUOTES_PATTERN =
      Pattern.compile("\\{[Qq]uote}([^+]*?)\\{[Qq]uote}", Pattern.MULTILINE);
  static final Pattern MENTION_PATTERN = Pattern.compile("\\[~(.*?)]");
  static final Pattern STRIKETHROUGH_PATTERN =
      Pattern.compile("(^|\\s)-([^- \\n].*?[^- \\n])-($|\\s|\\.)");
  static final Pattern MULTILEVEL_NUMBERED_LIST_PATTERN =
      Pattern.compile("^((?:#|-|\\+|\\*)+) (.*)$", Pattern.MULTILINE);
  static final Pattern BOLD_PATTERN = Pattern.compile("(^|)\\*([^*].*?[^*])\\*($||\\.)");
  static final Pattern UNDERLINE_PATTERN =
      Pattern.compile("(^|\\s)\\+([^+ \\n].*?[^+ \\n])\\+($|\\s|\\.)");
  static final Pattern LINK_PATTERN = Pattern.compile("\\[([^~|?\n]+)\\|(.+?)]");
  static final Pattern ITALIC_PATTERN =
      Pattern.compile("(^|\\s)_([^_ \\n].*?[^_ \\n])_($|\\s|\\.)");
  static final Pattern SINGLE_CHARACHTERS_PATTERN =
      Pattern.compile("(?<!±)([`{}+|@\\[\\]()~\\-*_])");
  static final Pattern MARKED_CHARACHTERS_PATTERN = Pattern.compile("±([`{}+|@\\[\\]()~\\-*_])");
  static final Pattern PANEL_PATTERN =
      Pattern.compile("\\{[Pp]anel([^}]*)}([^+]*?)\\{[Pp]anel}", Pattern.MULTILINE);

  private JiraMarkdownTextPattern() throws IllegalAccessException {
    throw new IllegalAccessException();
  }
}
