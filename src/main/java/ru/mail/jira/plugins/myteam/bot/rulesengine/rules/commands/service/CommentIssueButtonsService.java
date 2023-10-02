/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@RequiredArgsConstructor
public class CommentIssueButtonsService {
  private final UserChatService userChatService;

  public void sendMessageWithButtonsToCommentIssue(
      final ChatMessageEvent event, final List<String> issuesKey)
      throws MyteamServerErrorException, IOException {
    userChatService.sendMessageText(
        event.getChatId(),
        userChatService.getRawText(
            "ru.mail.jira.plugins.myteam.messageQueueProcessor.comment.chatHasManyLinkedIssues"),
        createButtons(issuesKey));
  }

  private List<List<InlineKeyboardMarkupButton>> createButtons(final List<String> issuesKey) {
    final List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>(issuesKey.size());
    buttons.addAll(
        issuesKey.stream().map(this::mapIssueKeyToButton).collect(Collectors.toUnmodifiableList()));
    return Collections.unmodifiableList(buttons);
  }

  private List<InlineKeyboardMarkupButton> mapIssueKeyToButton(final String issueKey) {
    return Collections.singletonList(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            issueKey, String.join("-", ButtonRuleType.CommentIssueByCommand.getName(), issueKey)));
  }
}
