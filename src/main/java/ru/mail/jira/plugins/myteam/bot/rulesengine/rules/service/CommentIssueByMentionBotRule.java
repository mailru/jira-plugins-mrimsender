/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.JiraKeyUtils;
import java.util.Collections;
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
import ru.mail.jira.plugins.myteam.commons.exceptions.ValidationException;
import ru.mail.jira.plugins.myteam.component.comment.create.CommentCreateArg;
import ru.mail.jira.plugins.myteam.i18n.JiraBotI18nProperties;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "Create comment by mention bot", description = "Create comment by mention bot")
@Slf4j
public class CommentIssueByMentionBotRule extends ChatAdminRule {
  private final MyteamApiClient myteamApiClient;
  private final IssueService issueService;

  public CommentIssueByMentionBotRule(
      final UserChatService userChatService,
      final RulesEngine rulesEngine,
      final MyteamApiClient myteamApiClient,
      final IssueService issueService) {
    super(userChatService, rulesEngine);
    this.myteamApiClient = myteamApiClient;
    this.issueService = issueService;
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
  public void execute(@Fact("event") final ChatMessageEvent event) {
    ApplicationUser jiraUserFromUserChatId =
        userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (Objects.isNull(jiraUserFromUserChatId)) {
      return;
    }
    final List<String> issueKeysFromString =
        JiraKeyUtils.getIssueKeysFromString(event.getMessage());
    log.error("issue key extracted: {}", issueKeysFromString);
    if (issueKeysFromString.size() == 0) {
      sendMessageWithRawText(
          event, JiraBotI18nProperties.COMMENT_NOT_HAS_ISSUE_KEY_IN_EVENT_MESSAGE_KEY);
      return;
    }

    if (issueKeysFromString.size() > 1) {
      sendMessageWithRawText(
          event, JiraBotI18nProperties.SEND_MESSAGE_WITH_ONE_ISSUE_KEY_MESSAGE_KEY);
      return;
    }

    issueKeysFromString.stream()
        .findFirst()
        .flatMap(issueKey -> findIssueByKey(issueKey, event))
        .ifPresent(
            issueForCommenting ->
                tryCommentIssue(issueForCommenting, event, jiraUserFromUserChatId));
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
      @NotNull ApplicationUser commentAuthor) {
    try {
      String message = event.getMessage();
      final CommentCreateArg commentCreateArg =
          new CommentCreateArg(
              issue,
              commentAuthor,
              Objects.nonNull(event.getMessageParts())
                  ? event.getMessageParts()
                  : Collections.emptyList(),
              Const.DEFAULT_ISSUE_QUOTE_MESSAGE_TEMPLATE,
              removeCommentedIssueWithKeyFromMainMessage(
                  removeCommentCommandFromMainMessage(removeBotMentionsFromMainMessage(message)),
                  issue.getKey()));
      issueService.commentIssue(commentCreateArg);
      sendMessageWithRawText(event, JiraBotI18nProperties.COMMENT_CREATED_SUCCESSFUL_MESSAGE_KEY);
    } catch (NoPermissionException e) {
      log.error("user with id [%s] has not permission create comment", e);
      sendMessageWithRawText(
          event, JiraBotI18nProperties.CREATE_COMMENT_NOT_HAVE_PERMISSION_MESSAGE_KEY);
    } catch (ValidationException e) {
      log.error("user with id [%s] sent not valid comment", e);
      sendMessageWithFormattedText(
          event, JiraBotI18nProperties.COMMENT_VALIDATION_ERROR_MESSAGE_KEY, e.getMessage());
    }
  }

  private String removeBotMentionsFromMainMessage(String formattedMainMessage) {
    return formattedMainMessage
        .replaceAll(String.format("@\\[%s\\]", myteamApiClient.getBotId()), "")
        .trim();
  }

  private String removeCommentCommandFromMainMessage(String formattedMainMessage) {
    return formattedMainMessage.replaceFirst(Const.COMMENT_ISSUE_BY_MENTION_BOT, "").trim();
  }

  private String removeCommentedIssueWithKeyFromMainMessage(
      String formattedMainMessage, @NotNull String issueKey) {
    return formattedMainMessage.replaceFirst(issueKey, "").trim();
  }

  private boolean isValidReceivedCommandForRule(
      final String command, final MyteamEvent event, final boolean isGroup, final String tag) {
    if (!(event instanceof ChatMessageEvent)) {
      return false;
    }
    return isGroup
        && CommandRuleType.CommentIssueByMentionBot.getName().equals(command)
        && "".equals(tag)
        && isBotMentioned((ChatMessageEvent) event);
  }

  private boolean isBotMentioned(ChatMessageEvent chatMessageEvent) {
    String botId = myteamApiClient.getBotId();
    return isBotMentionedInMainMessage(chatMessageEvent, botId)
        || isBotAuthorAtLeastReplyMessage(chatMessageEvent, botId);
  }

  private boolean isBotMentionedInMainMessage(ChatMessageEvent event, String botId) {
    String botMentionFormatInMessage = String.format("@[%s]", botId);
    return event.getMessage().contains(botMentionFormatInMessage);
  }

  private boolean isBotAuthorAtLeastReplyMessage(ChatMessageEvent chatMessageEvent, String botId) {
    final List<Part> messageParts = chatMessageEvent.getMessageParts();
    if (messageParts == null || messageParts.size() == 0) {
      return false;
    }

    for (Part messagePart : messageParts) {
      User messagePartFrom = null;
      if (messagePart instanceof Reply) {
        messagePartFrom = ((Reply) messagePart).getMessage().getFrom();
      }
      if (messagePart instanceof Forward) {
        messagePartFrom = ((Forward) messagePart).getMessage().getFrom();
      }
      if (messagePartFrom == null) {
        continue;
      }

      if (messagePartFrom.getUserId().equals(botId)) {
        return true;
      }
    }

    return false;
  }
}
