/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service;

import static ru.mail.jira.plugins.myteam.commons.Const.COMMENT_ISSUE_BY_MENTION_BOT;
import static ru.mail.jira.plugins.myteam.i18n.JiraBotI18nProperties.*;

import com.atlassian.jira.util.JiraKeyUtils;
import java.util.List;
import javax.naming.NoPermissionException;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.ChatAdminRule;
import ru.mail.jira.plugins.myteam.commons.exceptions.ValidationException;
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
    JiraKeyUtils.getIssueKeysFromString(event.getMessage()).stream()
        .findFirst()
        .ifPresentOrElse(
            issueKey -> tryCommentIssue(issueKey, event),
            () -> sendMessageWithRawText(event, COMMENT_NO_HAS_ISSUE_KEY_IN_EVENT_MESSAGE_KEY));
  }

  private void tryCommentIssue(String issueKey, ChatMessageEvent event) {
    try {
      // todo: нужно ли форматировать ссылки(под маской) в тексте коммента и что вообще в коммент
      // попасть должно?
      //  сообщения reply и forward брать?
      issueService.commentIssue(
          issueKey, userChatService.getJiraUserFromUserChatId(event.getUserId()), event);
      sendMessageWithRawText(event, COMMENT_CREATED_SUCCESSFUL_MESSAGE_KEY);
    } catch (NoPermissionException e) {
      log.error("user with id [%s] has not permission create comment", e);
      sendMessageWithRawText(event, COMMENT_CREATED_SUCCESSFUL_MESSAGE_KEY);
    } catch (ValidationException e) {
      log.error("user with id [%s] sent not valid comment", e);
      sendMessageWithFormattedText(event, COMMENT_CREATED_SUCCESSFUL_MESSAGE_KEY, e.getMessage());
    }
  }

  private boolean isValidReceivedCommandForRule(
      final String command, final MyteamEvent event, final boolean isGroup, final String tag) {
    if (!(event instanceof ChatMessageEvent)) {
      return false;
    }
    return isGroup
        && CommandRuleType.CommentIssueByMentionBot.equalsName(command)
        && COMMENT_ISSUE_BY_MENTION_BOT.equals(tag)
        && isBotMentioned((ChatMessageEvent) event);
  }

  private boolean isBotMentioned(ChatMessageEvent chatMessageEvent) {
    String botId = myteamApiClient.getBotId();
    return isBotMentionedInMainMessage(chatMessageEvent, botId)
        || isBotAuthorAtLeastReplyMessage(chatMessageEvent, botId);
  }

  private boolean isBotMentionedInMainMessage(ChatMessageEvent event, String botId) {
    String botMentionFormatInMessage = String.format("@%s", botId);
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
