/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.JiraKeyUtils;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.naming.NoPermissionException;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jetbrains.annotations.NotNull;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.ChatAdminRule;
import ru.mail.jira.plugins.myteam.commons.Const;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.commons.exceptions.ValidationException;
import ru.mail.jira.plugins.myteam.component.EventMessagesTextConverter;
import ru.mail.jira.plugins.myteam.component.comment.create.CommentCreateArg;
import ru.mail.jira.plugins.myteam.db.repository.MyteamChatRepository;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "Create comment by mention bot", description = "Create comment by mention bot")
@Slf4j
public class CommentIssueCommandBotRule extends ChatAdminRule {
  private final MyteamApiClient myteamApiClient;
  private final IssueService issueService;

  private final EventMessagesTextConverter messagePartProcessor;
  private final MyteamChatRepository myteamChatRepository;

  public CommentIssueCommandBotRule(
      final UserChatService userChatService,
      final RulesEngine rulesEngine,
      final MyteamApiClient myteamApiClient,
      final IssueService issueService,
      final EventMessagesTextConverter messagePartProcessor,
      final MyteamChatRepository myteamChatRepository) {
    super(userChatService, rulesEngine);
    this.myteamApiClient = myteamApiClient;
    this.issueService = issueService;
    this.messagePartProcessor = messagePartProcessor;
    this.myteamChatRepository = myteamChatRepository;
  }

  @Condition
  public boolean isValid(
      @Fact("command") String command,
      @Fact("event") MyteamEvent event,
      @Fact("isGroup") boolean isGroup,
      @Fact("args") String tag)
      throws AdminRulesRequiredException {

    return isValidReceivedCommandForRule(command, event, isGroup, tag);
  }

  @Action
  public void execute(@Fact("event") final ChatMessageEvent event)
      throws MyteamServerErrorException, IOException {
    ApplicationUser jiraUserFromUserChatId =
        userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (Objects.isNull(jiraUserFromUserChatId)) {
      return;
    }
    final List<String> issueKeysFromString =
        JiraKeyUtils.getIssueKeysFromString(event.getMessage());
    if (issueKeysFromString.size() == 0) {
      Optional<Issue> issueOptional =
          Optional.ofNullable(myteamChatRepository.findChatByChatId(event.getChatId()))
              .flatMap(myteamChatMeta -> findIssueByKey(myteamChatMeta.getIssueKey(), event));
      if (issueOptional.isPresent()) {
        tryCommentIssue(issueOptional.get(), event, jiraUserFromUserChatId);
      }
      return;
    }

    Optional<Issue> issueOptional =
        issueKeysFromString.stream()
            .findFirst()
            .flatMap(issueKey -> findIssueByKey(issueKey, event));
    if (issueOptional.isPresent()) {
      tryCommentIssue(issueOptional.get(), event, jiraUserFromUserChatId);
    }
  }

  private Optional<Issue> findIssueByKey(
      @NotNull final String issueKey, @NotNull final ChatMessageEvent event) {
    try {
      return Optional.of(issueService.getIssue(issueKey));
    } catch (IssueNotFoundException e) {
      rulesEngine.fireError(ErrorRuleType.IssueNotFound, event, e);
      return Optional.empty();
    }
  }

  private void tryCommentIssue(
      @NotNull final Issue issue,
      @NotNull ChatMessageEvent event,
      @NotNull ApplicationUser commentAuthor)
      throws MyteamServerErrorException, IOException {
    try {
      final CommentCreateArg commentCreateArg =
          new CommentCreateArg(
              issue, commentAuthor, createCommentBodyFromEvent(event, issue, commentAuthor));
      issueService.commentIssue(commentCreateArg);
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.commentCreated"));
    } catch (NoPermissionException e) {
      log.error("user with id [%s] has not permission create comment", e);
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.noPermissions"));
    } catch (ValidationException e) {
      log.error("user with id [%s] sent not valid comment", e);
      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getText(
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.commentValidationFailed",
              e.getMessage()));
    }
  }

  private String createCommentBodyFromEvent(
      ChatMessageEvent event, final Issue issueToComment, final ApplicationUser commentAuthor) {
    return messagePartProcessor
        .convertMessagesFromReplyAndForwardMessages(
            event::getMessageParts, issueToComment, Const.DEFAULT_ISSUE_QUOTE_MESSAGE_TEMPLATE)
        .map(markdownFieldValueHolder -> new StringBuilder(markdownFieldValueHolder.getValue()))
        .map(
            commentBodyBuilder -> {
              String convertedMainMessage =
                  messagePartProcessor.convertToJiraMarkdownStyleMainMessage(
                      event, commentAuthor, issueToComment);
              if (Objects.nonNull(convertedMainMessage)) {
                commentBodyBuilder
                    .append("\n\n")
                    .append(
                        removeBotMentionAndCommandAndCommentedIssueWithKeyFromMainMessage(
                            convertedMainMessage, issueToComment));
              }
              return commentBodyBuilder.toString();
            })
        .orElse(
            removeBotMentionAndCommandAndCommentedIssueWithKeyFromMainMessage(
                messagePartProcessor.convertToJiraMarkdownStyleMainMessage(
                    event, commentAuthor, issueToComment),
                issueToComment));
  }

  private String removeBotMentionAndCommandAndCommentedIssueWithKeyFromMainMessage(
      String formattedMainMessage, Issue issue) {
    return removeCommentedIssueWithKeyFromMainMessage(
        removeCommentCommandFromMainMessage(removeBotMentionsFromMainMessage(formattedMainMessage)),
        issue.getKey());
  }

  private String removeBotMentionsFromMainMessage(String formattedMainMessage) {
    return formattedMainMessage
        .replaceAll(String.format("@\\[%s\\]", myteamApiClient.getBotId()), "")
        .trim();
  }

  private String removeCommentCommandFromMainMessage(String formattedMainMessage) {
    return formattedMainMessage.replaceFirst(Const.COMMENT_ISSUE_COMMAND, "").trim();
  }

  private String removeCommentedIssueWithKeyFromMainMessage(
      String formattedMainMessage, @NotNull String issueKey) {
    return formattedMainMessage.replaceFirst(issueKey, "").trim();
  }

  private boolean isValidReceivedCommandForRule(
      final String command, final MyteamEvent event, boolean isGroup, final String tag) {
    return isGroup
        && CommandRuleType.CommentIssueByMentionBot.getName().equals(command)
        && "".equals(tag)
        && event instanceof ChatMessageEvent
        && (isBotMentioned((ChatMessageEvent) event)
            || isEventFromChatLinkedToIssue((ChatMessageEvent) event));
  }

  private boolean isBotMentioned(final ChatMessageEvent chatMessageEvent) {
    String botId = myteamApiClient.getBotId();
    return isBotMentionedInMainMessage(chatMessageEvent, botId);
  }

  private boolean isEventFromChatLinkedToIssue(final ChatMessageEvent chatMessageEvent) {
    return myteamChatRepository.findChatByChatId(chatMessageEvent.getChatId()) != null;
  }

  private boolean isBotMentionedInMainMessage(final ChatMessageEvent event, final String botId) {
    String botMentionFormatInMessage = String.format("@[%s]", botId);
    return event.getMessage().contains(botMentionFormatInMessage);
  }
}
