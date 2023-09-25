/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service;

import static ru.mail.jira.plugins.myteam.commons.Const.CHAT_COMMAND_PREFIX;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.JiraKeyUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.naming.NoPermissionException;
import kong.unirest.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.ChatAdminRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.CommentingIssueFromGroupChatState;
import ru.mail.jira.plugins.myteam.commons.Const;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.commons.exceptions.ValidationException;
import ru.mail.jira.plugins.myteam.component.EventMessagesTextConverter;
import ru.mail.jira.plugins.myteam.component.comment.create.CommentCreateArg;
import ru.mail.jira.plugins.myteam.db.model.MyteamChatMeta;
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

  private static final String COMMENT_COMMAND =
      CHAT_COMMAND_PREFIX + CommandRuleType.CommentIssueByMentionBot.getName();

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
      @Fact("args") String args)
      throws AdminRulesRequiredException {

    return isGroup
        && CommandRuleType.CommentIssueByMentionBot.getName().equals(command)
        && event instanceof ChatMessageEvent
        && (isBotMentioned((ChatMessageEvent) event)
            || isEventFromChatLinkedToIssue((ChatMessageEvent) event)
            || isEventSendFromCommandStateRule(args));
  }

  @Action
  public void execute(@Fact("event") final ChatMessageEvent event, @Fact("args") String args)
      throws MyteamServerErrorException, IOException {
    ApplicationUser jiraUserFromUserChatId =
        userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (Objects.isNull(jiraUserFromUserChatId)) {
      return;
    }

    final String issueKeySentFromCommandStateRule = getIssueKeySentFromCommandStateRule(args);
    if (issueKeySentFromCommandStateRule != null) {
      final Optional<Issue> issueOptional =
          Optional.ofNullable(findIssueByKey(issueKeySentFromCommandStateRule, event))
              .flatMap(issue -> issue);
      if (issueOptional.isPresent()) {
        tryCommentIssue(issueOptional.get(), event, jiraUserFromUserChatId);
      }
      return;
    }

    final List<String> issueKeysFromString =
        JiraKeyUtils.getIssueKeysFromString(event.getMessage());

    if (issueKeysFromString.size() == 0) {
      MyteamChatMeta[] chatByChatId = myteamChatRepository.findChatByChatId(event.getChatId());
      if (chatByChatId == null || chatByChatId.length == 0) {
        return;
      }
      if (chatByChatId.length > 1) {
        CommentingIssueFromGroupChatState commentingIssueFromGroupChatRule =
            new CommentingIssueFromGroupChatState(event);
        commentingIssueFromGroupChatRule.setWaiting(true);
        userChatService.setState(event.getChatId(), commentingIssueFromGroupChatRule);
        userChatService.sendMessageText(
            event.getChatId(),
            userChatService.getText(
                "ru.mail.jira.plugins.myteam.messageQueueProcessor.comment.chatHasManyLinkedIssues",
                Arrays.stream(chatByChatId)
                    .map(MyteamChatMeta::getIssueKey)
                    .collect(Collectors.joining(", "))));
        return;
      }

      final Optional<Issue> issueOptional =
          Optional.ofNullable(findIssueByKey(chatByChatId[0].getIssueKey(), event))
              .flatMap(issue -> issue);
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
    return formattedMainMessage
        .replaceAll(String.format("@\\[%s\\]", myteamApiClient.getBotId()), "")
        .replaceFirst(CommentIssueCommandBotRule.COMMENT_COMMAND, "")
        .replaceFirst(issue.getKey(), "")
        .trim();
  }

  private boolean isBotMentioned(final ChatMessageEvent chatMessageEvent) {
    String botId = myteamApiClient.getBotId();
    return isBotMentionedInMainMessage(chatMessageEvent, botId);
  }

  private boolean isEventFromChatLinkedToIssue(final ChatMessageEvent chatMessageEvent) {
    MyteamChatMeta[] chatByChatId =
        myteamChatRepository.findChatByChatId(chatMessageEvent.getChatId());
    return chatByChatId != null && chatByChatId.length > 1;
  }

  private boolean isBotMentionedInMainMessage(final ChatMessageEvent event, final String botId) {
    String botMentionFormatInMessage = String.format("@[%s]", botId);
    return event.getMessage().contains(botMentionFormatInMessage);
  }

  private boolean isEventSendFromCommandStateRule(final String arg) {
    try {
      return new JSONObject(arg).getBoolean("issueKeyReceivedFromNewEvent");
    } catch (Exception e) {
      return false;
    }
  }

  @Nullable
  private String getIssueKeySentFromCommandStateRule(final String arg) {
    try {
      return new JSONObject(arg).getString("issueKey");
    } catch (Exception e) {
      return null;
    }
  }
}
