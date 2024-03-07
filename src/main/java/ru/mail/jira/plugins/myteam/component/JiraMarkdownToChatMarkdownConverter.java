/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import static ru.mail.jira.plugins.myteam.commons.Utils.shieldText;

import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Component
public class JiraMarkdownToChatMarkdownConverter {

  private final UserManager userManager;
  private final I18nHelper i18nHelper;
  private final PluginData pluginData;

  public JiraMarkdownToChatMarkdownConverter(
      @ComponentImport final UserManager userManager,
      @ComponentImport final I18nHelper i18nHelper,
      final PluginData pluginData) {
    this.userManager = userManager;
    this.i18nHelper = i18nHelper;
    this.pluginData = pluginData;
  }

  @Nullable
  public String makeMyteamMarkdownFromJira(
      @Nullable String inputText, final boolean useMentionFormat) {
    if (inputText == null) {
      return null;
    }
    // remove carriage return
    inputText = inputText.replaceAll("\r", "");

    // codeBlockPattern
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.CODE_BLOCK_PATTERN_1,
            input -> "\n±`±`±`" + shieldText(input.group(1) + " " + input.group(2)) + "±`±`±`");
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.CODE_BLOCK_PATTERN_2,
            input -> "\n±`±`±`" + shieldText(input.group(1)) + "±`±`±`");
    // inlineCodePattern
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.INLINE_CODE_PATTERN,
            input -> "±`" + input.group(1) + "±`");
    // Quote
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.QUOTES_PATTERN,
            input -> "\n>" + input.group(1).replaceAll("\n", "\n>") + "\n");
    // mentionPattern
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.MENTION_PATTERN,
            input -> convertMentionUser(useMentionFormat, input));
    // strikethroughtPattern
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.STRIKETHROUGH_PATTERN,
            input -> input.group(1) + "±~" + input.group(2) + "±~" + input.group(3));
    // multi level numbered list
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.MULTILEVEL_NUMBERED_LIST_PATTERN,
            input -> "±- " + input.group(2));
    // bold Pattern
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.BOLD_PATTERN,
            input -> input.group(1) + "±*" + input.group(2) + "±*" + input.group(3));
    // underLinePattern
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.UNDERLINE_PATTERN,
            input -> input.group(1) + "±_±_" + input.group(2) + "±_±_" + input.group(3));
    // linkPattern
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.LINK_PATTERN,
            input -> "±[" + shieldText(input.group(1)) + "±]±(" + input.group(2) + "±)");
    // Italic
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.ITALIC_PATTERN,
            input -> input.group(1) + "±_" + input.group(2) + "±_" + input.group(3));
    // Single characters
    inputText = addShieldToNonShieldSingleChars(inputText);
    // Marked characters
    inputText =
        convertToMarkdown(
            inputText, JiraMarkdownTextPattern.MARKED_CHARACHTERS_PATTERN, input -> input.group(1));

    return inputText;
  }

  private String convertMentionUser(final boolean useMentionFormat, final Matcher input) {
    ApplicationUser mentionUser = userManager.getUserByName(input.group(1));
    if (mentionUser != null) {
      if (useMentionFormat) {
        return "±@\\±[" + shieldText(mentionUser.getEmailAddress()) + "\\±]";
      }
      return "±["
          + shieldText(mentionUser.getDisplayName())
          + "±]±("
          + shieldText(pluginData.getProfileLink() + mentionUser.getEmailAddress())
          + "±)";
    } else {
      return i18nHelper.getText("common.words.anonymous");
    }
  }

  public static String convertToMarkdown(
      final String inputText, final Pattern pattern, final Function<Matcher, String> converter) {
    int lastIndex = 0;
    final StringBuilder output = new StringBuilder();
    final Matcher matcher = pattern.matcher(inputText);
    while (matcher.find()) {
      output.append(inputText, lastIndex, matcher.start()).append(converter.apply(matcher));
      lastIndex = matcher.end();
    }
    if (lastIndex < inputText.length()) {
      output.append(inputText, lastIndex, inputText.length());
    }
    return output.toString();
  }

  @NotNull
  private static String addShieldToNonShieldSingleChars(@NotNull String inputText) {
    AtomicReference<@NotNull String> ref = new AtomicReference<>();
    ref.set(inputText);
    inputText =
        convertToMarkdown(
            inputText,
            JiraMarkdownTextPattern.SINGLE_CHARACHTERS_PATTERN,
            input -> {
              // for compiler
              String sourceText = ref.get();
              if (sourceText == null) {
                return "";
              }
              if (input.start() != 0) {
                char c = sourceText.charAt(input.start() - 1);
                if (c != '\\') {
                  return '\\' + input.group(1);
                } else {
                  return input.group(1);
                }
              } else {
                return shieldText(input.group(1));
              }
            });
    return inputText;
  }
}
