/* (C)2023 */
package ru.mail.jira.plugins.myteam.component;

import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service.CreateIssueByReplyRule.DATE_TIME_FORMATTER;
import static ru.mail.jira.plugins.myteam.commons.Const.DEFAULT_ISSUE_QUOTE_MESSAGE_TEMPLATE;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.user.ApplicationUser;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.component.url.UrlFinderInForward;
import ru.mail.jira.plugins.myteam.component.url.UrlFinderInMainEvent;
import ru.mail.jira.plugins.myteam.component.url.UrlFinderInReply;
import ru.mail.jira.plugins.myteam.component.url.dto.LinksInMessage;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;

@Component
public class EventMessagesTextConverter {
  private final UrlFinderInForward urlFinderInForward;
  private final UrlFinderInReply urlFinderInReply;

  private final UrlFinderInMainEvent urlFinderInMainEvent;
  private final MessageFormatter messageFormatter;
  private final IssueTextConverter issueTextConverter;

  public EventMessagesTextConverter(
      final UrlFinderInForward urlFinderInForward,
      final UrlFinderInReply urlFinderInReply,
      UrlFinderInMainEvent urlFinderInMainEvent,
      MessageFormatter messageFormatter,
      IssueTextConverter issueTextConverter) {
    this.urlFinderInForward = urlFinderInForward;
    this.urlFinderInReply = urlFinderInReply;
    this.urlFinderInMainEvent = urlFinderInMainEvent;
    this.messageFormatter = messageFormatter;
    this.issueTextConverter = issueTextConverter;
  }

  public Optional<MarkdownFieldValueHolder> convertMessagesFromReplyAndForwardMessages(
      @NotNull Supplier<List<Part>> partsProvider,
      @Nullable Issue issue,
      @Nullable String template) {
    return ofNullable(partsProvider.get())
        .filter(not(List::isEmpty))
        .map(parts -> convertToJiraMarkdownStyle(parts, template, issue));
  }

  @Nullable
  private MarkdownFieldValueHolder convertToJiraMarkdownStyle(
      List<Part> parts, @Nullable String template, @Nullable Issue issue) {
    StringBuilder builder = new StringBuilder();
    final List<LinksInMessage> linksInMessages = new ArrayList<>();
    List<Part> replyOrForwardMessages =
        parts.stream()
            .filter(part -> (part instanceof Reply || part instanceof Forward))
            .collect(Collectors.toUnmodifiableList());
    if (replyOrForwardMessages.isEmpty()) {
      return null;
    }
    replyOrForwardMessages.forEach(
        p -> {
          User user;
          long timestamp;
          String text;
          LinksInMessage urls;
          if (p instanceof Reply) {
            Reply reply = (Reply) p;
            user = reply.getMessage().getFrom();
            timestamp = reply.getMessage().getTimestamp();
            urls = urlFinderInReply.findUrls(reply);
            text = reply.getPayload().getMessage().getText();
          } else {
            Forward forward = (Forward) p;
            user = forward.getMessage().getFrom();
            timestamp = forward.getMessage().getTimestamp();
            urls = urlFinderInForward.findUrls(forward);
            text = forward.getPayload().getMessage().getText();
          }
          linksInMessages.add(urls);

          if (issue != null) {
            text =
                issueTextConverter.convertToJiraDescriptionAndCommentMarkdownStyle(
                    p, issue, textFromPart -> messageFormatter.formatLinks(textFromPart, urls));
          } else {
            text = messageFormatter.formatLinks(text, urls);
          }

          String resultTemplate = template;
          if (resultTemplate == null) {
            resultTemplate = DEFAULT_ISSUE_QUOTE_MESSAGE_TEMPLATE;
          }

          if (user != null) {
            builder.append(messageFormatter.formatMyteamUserLink(user));
          }
          builder
              .append("(")
              .append(
                  DATE_TIME_FORMATTER.format(
                      LocalDateTime.ofInstant(
                          Instant.ofEpochSecond(timestamp), TimeZone.getDefault().toZoneId())))
              .append("):\n")
              .append("\n");
          builder.append(StringUtils.replace(resultTemplate, "{{message}}", text));
          builder.append("\n\n");
        });
    return new MarkdownFieldValueHolder(builder.toString(), linksInMessages);
  }

  public String convertToJiraMarkdownStyleMainMessage(
      ChatMessageEvent event, ApplicationUser commentAuthor, Issue issueToComment) {
    final String mainMessageText = event.getMessage();
    final LinksInMessage linksInMessage = urlFinderInMainEvent.findUrls(event);
    final String mainMessageTextWithUrls =
        messageFormatter.formatLinks(mainMessageText, linksInMessage);
    if (event.getMessageParts() != null) {
      return issueTextConverter.convertToJiraCommentStyle(
          event, mainMessageTextWithUrls, commentAuthor, issueToComment);
    }
    return mainMessageTextWithUrls;
  }

  @Nullable
  public String getIssueCommentAsLink(final Issue issue, final Comment comment) {
    return messageFormatter.formatJiraIssueCommentToLink(issue, comment);
  }

  @NotNull
  public String convertToJiraMarkdownStyleMainMessage(ChatMessageEvent event) {
    final String mainMessageText = event.getMessage();
    final LinksInMessage linksInMessage = urlFinderInMainEvent.findUrls(event);
    return issueTextConverter.replaceChatUserMentionToJiraUserMention(
        messageFormatter.formatLinks(mainMessageText, linksInMessage), event);
  }

  @Data
  public static final class MarkdownFieldValueHolder {
    private final String value;
    private final List<LinksInMessage> linksInMessages;
  }
}
