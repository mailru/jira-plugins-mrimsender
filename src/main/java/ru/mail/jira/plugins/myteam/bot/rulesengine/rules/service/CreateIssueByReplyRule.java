/* (C)2022 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service;

import static ru.mail.jira.plugins.myteam.commons.Const.*;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.ChatAdminRule;
import ru.mail.jira.plugins.myteam.commons.IssueReporter;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.IssueTextConverter;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;
import ru.mail.jira.plugins.myteam.service.*;

@Slf4j
@Rule(
    name = "Create issue by reply",
    description = "Create issue by reply if feature has been setup")
public class CreateIssueByReplyRule extends ChatAdminRule {

  static final RuleType NAME = CommandRuleType.CreateIssueByReply;

  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  private final IssueCreationSettingsService issueCreationSettingsService;
  private final IssueCreationService issueCreationService;
  private final IssueService issueService;
  private final IssueTextConverter issueTextConverter;

  public CreateIssueByReplyRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueCreationSettingsService issueCreationSettingsService,
      IssueCreationService issueCreationService,
      IssueService issueService,
      IssueTextConverter issueTextConverter) {
    super(userChatService, rulesEngine);
    this.issueCreationSettingsService = issueCreationSettingsService;
    this.issueCreationService = issueCreationService;
    this.issueService = issueService;
    this.issueTextConverter = issueTextConverter;
  }

  @Condition
  public boolean isValid(
      @Fact("command") String command,
      @Fact("event") MyteamEvent event,
      @Fact("isGroup") boolean isGroup,
      @Fact("args") String tag)
      throws AdminRulesRequiredException {
    IssueCreationSettingsDto settings =
        issueCreationSettingsService.getSettingsFromCache(event.getChatId(), tag);

    if (isGroup
        && NAME.equalsName(command)
        && event instanceof ChatMessageEvent
        && settings != null) {
      if (!settings.getCreationByAllMembers()) {
        checkAdminRules(event);
      }
      return true;
    } else {
      return false;
    }
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event, @Fact("args") String tag)
      throws MyteamServerErrorException, IOException {
    ApplicationUser initiator = null;
    try {
      IssueCreationSettingsDto settings =
          issueCreationSettingsService.getSettingsFromCache(event.getChatId(), tag);

      if (settings == null || !issueCreationSettingsService.hasRequiredFields(settings)) {
        return;
      }

      List<User> reporters = getReportersFromEventParts(event);

      User firstMessageReporter = reporters.size() > 0 ? reporters.get(0) : event.getFrom();

      initiator = userChatService.getJiraUserFromUserChatId(event.getUserId());

      if (initiator == null || firstMessageReporter == null) {
        return;
      }

      HashMap<Field, String> fieldValues = new HashMap<>();

      String summary =
          getIssueSummary(
              event,
              settings.getIssueSummaryTemplate(),
              firstMessageReporter,
              event.getFrom(),
              tag);
      fieldValues.put(issueCreationService.getField(IssueFieldConstants.SUMMARY), summary);

      settings
          .getAdditionalFields()
          .forEach(
              f -> {
                Field field = issueCreationService.getField(f.getField());
                if (field != null) {
                  if (fieldValues.containsKey(field)) {
                    fieldValues.put(
                        field, String.format("%s, %s", fieldValues.get(field), f.getValue()));
                  } else {
                    fieldValues.put(field, f.getValue());
                  }
                }
              });

      fieldValues.put(
          issueCreationService.getField(IssueFieldConstants.DESCRIPTION),
          getIssueDescription(event, settings.getIssueQuoteMessageTemplate(), null));

      String assigneeValue = settings.getAssignee();
      @Nullable ApplicationUser assigneeUser = null;

      switch (assigneeValue) {
        case "MESSAGE_AUTHOR":
          assigneeUser =
              userChatService.getJiraUserFromUserChatId(firstMessageReporter.getUserId());
          break;
        case "INITIATOR":
          assigneeUser = initiator;
          break;
      }

      String assigneeUsername = assigneeUser != null ? assigneeUser.getUsername() : assigneeValue;
      fieldValues.put(
          issueCreationService.getField(IssueFieldConstants.ASSIGNEE), assigneeUsername);

      if (settings.getLabels() != null) {
        fieldValues.put(
            issueCreationService.getField(IssueFieldConstants.LABELS),
            String.join(",", settings.getLabels()));
      }

      ApplicationUser reporterJiraUser =
          Optional.ofNullable(
                  userChatService.getJiraUserFromUserChatId(firstMessageReporter.getUserId()))
              .orElse(initiator);

      MutableIssue issue =
          issueCreationService.createIssue(
              settings.getProjectKey(),
              settings.getIssueTypeId(),
              fieldValues,
              settings.getReporter() == IssueReporter.MESSAGE_AUTHOR
                  ? reporterJiraUser
                  : initiator);

      issueCreationService.addIssueChatLink(
          issue, settings.getChatTitle(), settings.getChatLink(), initiator);

      issueCreationService.updateIssueDescription(
          getIssueDescription(event, settings.getIssueQuoteMessageTemplate(), issue),
          issue,
          reporterJiraUser);

      if (settings.getAddReporterInWatchers()) {
        reporters.stream() // add watchers
            .map(u -> userChatService.getJiraUserFromUserChatId(u.getUserId()))
            .filter(Objects::nonNull)
            .forEach(user -> issueService.watchIssue(issue, user));
      }

      userChatService.sendMessageText(
          event.getChatId(),
          getCreationSuccessMessage(settings.getCreationSuccessTemplate(), issue, summary));
    } catch (Exception e) {
      log.error(e.getLocalizedMessage(), e);
      SentryClient.capture(e);

      userChatService.sendMessageText(
          event.getUserId(),
          Utils.shieldText(
              String.format(
                  "Возникла ошибка при создании задачи.%n%n%s", e.getLocalizedMessage())));
    }
  }

  private String getCreationSuccessMessage(String template, Issue issue, String summary) {
    String result = template;
    if (result == null) {
      result = DEFAULT_ISSUE_CREATION_SUCCESS_TEMPLATE;
    }

    Map<String, String> keyMap = new HashMap<>();

    keyMap.put("issueKey", messageFormatter.createMarkdownIssueShortLink(issue.getKey()));
    keyMap.put("issueLink", messageFormatter.createIssueLink(issue.getKey()));
    keyMap.put("summary", summary);

    for (Map.Entry<String, String> entry : keyMap.entrySet()) {
      result = result.replaceAll(String.format("\\{\\{%s\\}\\}", entry.getKey()), entry.getValue());
    }

    return result;
  }

  private List<User> getReportersFromEventParts(ChatMessageEvent event) {
    if (event.getMessageParts() == null) {
      return ImmutableList.of();
    }
    return event.getMessageParts().stream()
        .filter(p -> (p instanceof Reply || p instanceof Forward))
        .map(
            p ->
                (p instanceof Reply)
                    ? ((Reply) p).getMessage().getFrom()
                    : ((Forward) p).getMessage().getFrom())
        .collect(Collectors.toList());
  }

  private String getIssueDescription(
      ChatMessageEvent event, String template, @Nullable Issue issue) {
    StringBuilder builder = new StringBuilder();

    if (event.getMessageParts() != null) {
      event.getMessageParts().stream()
          .filter(part -> (part instanceof Reply || part instanceof Forward))
          .forEach(
              p -> {
                User user;
                long timestamp;
                String text;
                if (p instanceof Reply) {
                  user = ((Reply) p).getMessage().getFrom();
                  timestamp = ((Reply) p).getMessage().getTimestamp();
                  text = ((Reply) p).getPayload().getMessage().getText();
                } else {
                  user = ((Forward) p).getMessage().getFrom();
                  timestamp = ((Forward) p).getMessage().getTimestamp();
                  text = ((Forward) p).getPayload().getMessage().getText();
                }

                if (issue != null) {
                  text = issueTextConverter.convertToJiraDescriptionStyle(p, issue);
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
                        formatter.format(
                            LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(timestamp),
                                TimeZone.getDefault().toZoneId())))
                    .append("):\n")
                    .append("\n");
                builder.append(StringUtils.replace(resultTemplate, "{{message}}", text));
                builder.append("\n\n");
              });
    }
    return builder.toString();
  }

  private String getIssueSummary(
      ChatMessageEvent event,
      String template,
      User firstMessageReporter,
      User initiator,
      String tag) {

    String message = event.getMessage();

    String botMention = String.format("@\\[%s\\]", userChatService.getBotId());
    message = message.replaceAll(botMention, "").trim();

    String comment =
        StringUtils.substringAfter(message, ISSUE_CREATION_BY_REPLY_PREFIX + tag).trim();
    if (comment.length() != 0) {
      return comment;
    }

    String result = template;
    if (result == null) {
      result = DEFAULT_ISSUE_SUMMARY_TEMPLATE;
    }

    Map<String, String> keyMap = new HashMap<>();

    keyMap.put("author", MessageFormatter.getUserDisplayName(firstMessageReporter));
    keyMap.put("initiator", MessageFormatter.getUserDisplayName(initiator));

    for (Map.Entry<String, String> entry : keyMap.entrySet()) {
      result = result.replaceAll(String.format("\\{\\{%s\\}\\}", entry.getKey()), entry.getValue());
    }

    return result;
  }
}
