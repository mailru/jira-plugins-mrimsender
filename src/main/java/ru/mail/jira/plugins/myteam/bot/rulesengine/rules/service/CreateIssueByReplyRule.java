/* (C)2022 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service;

import static java.util.Collections.emptyList;
import static ru.mail.jira.plugins.myteam.commons.Const.*;

import com.atlassian.jira.exception.NotFoundException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IssueCreationValidationException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.ChatAdminRule;
import ru.mail.jira.plugins.myteam.commons.IssueReporter;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.EventMessagesTextConverter;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;
import ru.mail.jira.plugins.myteam.component.url.dto.LinksInMessage;
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

  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
  private static final Splitter NEW_LINE_SPLITTER = Splitter.on("\n");

  private final IssueCreationSettingsService issueCreationSettingsService;
  private final IssueCreationService issueCreationService;
  private final IssueService issueService;

  private final EventMessagesTextConverter eventMessagesTextConverter;
  private final PermissionHelper permissionHelper;

  public CreateIssueByReplyRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueCreationSettingsService issueCreationSettingsService,
      IssueCreationService issueCreationService,
      IssueService issueService,
      EventMessagesTextConverter eventMessagesTextConverter,
      PermissionHelper permissionHelper) {
    super(userChatService, rulesEngine);
    this.issueCreationSettingsService = issueCreationSettingsService;
    this.issueCreationService = issueCreationService;
    this.issueService = issueService;
    this.eventMessagesTextConverter = eventMessagesTextConverter;
    this.permissionHelper = permissionHelper;
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
    try {
      IssueCreationSettingsDto settings =
          issueCreationSettingsService.getSettingsFromCache(event.getChatId(), tag);

      if (settings == null || !issueCreationSettingsService.hasRequiredFields(settings)) {
        return;
      }

      List<User> reporters = getReportersFromEventParts(event);

      User firstMessageReporter = reporters.size() > 0 ? reporters.get(0) : event.getFrom();

      ApplicationUser initiator = userChatService.getJiraUserFromUserChatId(event.getUserId());

      if (initiator == null || firstMessageReporter == null) {
        return;
      }

      ApplicationUser reporterJiraUser =
          resolveReporterJiraUser(firstMessageReporter, settings, initiator);

      HashMap<Field, String> fieldValues = new HashMap<>();

      SummaryAndDescriptionStatusFromMainMessage summary =
          getIssueSummary(
              event,
              settings.getIssueSummaryTemplate(),
              firstMessageReporter,
              event.getFrom(),
              tag);
      fieldValues.put(
          issueCreationService.getField(IssueFieldConstants.SUMMARY),
          summary.summary.replaceAll("\n", " "));

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

      EventMessagesTextConverter.MarkdownFieldValueHolder markdownFieldValueHolder =
          getIssueDescription(event, settings.getIssueQuoteMessageTemplate(), null);
      LinksInMessage linksInMessageInMainMessage =
          eventMessagesTextConverter.findLinksInMainMessage(event);
      String description;
      String descFromMainMessage =
          createFirstPartOfDescriptionByMainMessageText(event, linksInMessageInMainMessage, tag);
      if (summary.addDescFromParts) {
        description =
            buildFullDescFromMainAndReplyMessages(
                descFromMainMessage, markdownFieldValueHolder.getValue());
        fieldValues.put(
            issueCreationService.getField(IssueFieldConstants.DESCRIPTION), description);
      } else {
        description = descFromMainMessage;
        fieldValues.put(
            issueCreationService.getField(IssueFieldConstants.DESCRIPTION), description);
      }

      String assigneeValue = settings.getAssignee();
      if (assigneeValue != null) {
        switch (assigneeValue) {
          case "MESSAGE_AUTHOR":
            @Nullable
            ApplicationUser assigneeUser =
                userChatService.getJiraUserFromUserChatId(firstMessageReporter.getUserId());
            if (assigneeUser != null) {
              fieldValues.put(
                  issueCreationService.getField(IssueFieldConstants.ASSIGNEE),
                  assigneeUser.getUsername());
            } else
              throw new NotFoundException(
                  String.format("User `%s` not found", firstMessageReporter.getUserId()));

            break;
          case "INITIATOR":
            fieldValues.put(
                issueCreationService.getField(IssueFieldConstants.ASSIGNEE),
                initiator.getUsername());
            break;
          case "AUTO":
            fieldValues.put(issueCreationService.getField(IssueFieldConstants.ASSIGNEE), "-1");
            break;
          default:
            fieldValues.put(
                issueCreationService.getField(IssueFieldConstants.ASSIGNEE), assigneeValue);
        }
      }

      if (settings.getLabels() != null) {
        fieldValues.put(
            issueCreationService.getField(IssueFieldConstants.LABELS),
            String.join(",", settings.getLabels()));
      }

      MutableIssue issue =
          issueCreationService.createIssue(
              settings.getProjectKey(), settings.getIssueTypeId(), fieldValues, reporterJiraUser);

      if (Boolean.TRUE.equals(settings.getAllowedCreateChatLink())) {
        issueCreationService.addIssueChatLink(
            issue, settings.getChatTitle(), settings.getChatLink(), initiator);
      }

      if (summary.addDescFromParts) {
        issueCreationService.updateIssueDescription(
            buildFullDescFromMainAndReplyMessages(
                descFromMainMessage,
                getIssueDescription(event, settings.getIssueQuoteMessageTemplate(), issue)
                    .getValue()),
            issue,
            reporterJiraUser);
        issueCreationService.addLinksToIssueFromMessage(
            issue,
            Stream.concat(
                    Stream.of(linksInMessageInMainMessage),
                    markdownFieldValueHolder.getLinksInMessages().stream())
                .collect(Collectors.toList()),
            initiator);
      } else {
        issueCreationService.addLinksToIssueFromMessage(
            issue, Collections.singletonList(linksInMessageInMainMessage), initiator);
      }

      if (settings.getAddReporterInWatchers()) {
        reporters.stream() // add watchers
            .map(u -> userChatService.getJiraUserFromUserChatId(u.getUserId()))
            .filter(Objects::nonNull)
            .forEach(user -> issueService.watchIssue(issue, user));
      }

      userChatService.sendMessageText(
          event.getChatId(),
          getCreationSuccessMessage(settings.getCreationSuccessTemplate(), issue, summary.summary));
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

  @NotNull
  private ApplicationUser resolveReporterJiraUser(
      @NotNull User firstMessageReporter,
      @NotNull IssueCreationSettingsDto settings,
      @NotNull ApplicationUser initiator)
      throws IssueCreationValidationException {
    final Optional<ApplicationUser> reporterJiraUserOptional =
        Optional.ofNullable(
            userChatService.getJiraUserFromUserChatId(firstMessageReporter.getUserId()));
    ApplicationUser reporterJiraUser;
    if (reporterJiraUserOptional.isPresent()
        && settings.getReporter() == IssueReporter.MESSAGE_AUTHOR
        && permissionHelper.checkCreateIssuePermission(
            reporterJiraUserOptional.get(), settings.getProjectKey())) {
      reporterJiraUser = reporterJiraUserOptional.get();
    } else {
      if (permissionHelper.checkCreateIssuePermission(initiator, settings.getProjectKey())) {
        reporterJiraUser = initiator;
      } else {
        throw new IssueCreationValidationException(
            String.format("Unable to create issue in project %s", settings.getProjectKey()),
            reporterJiraUserOptional
                .map(
                    reporterUser ->
                        new SimpleErrorCollection(
                            String.format(
                                "Users %s and %s have not permission to create issue",
                                reporterUser, initiator),
                            ErrorCollection.Reason.FORBIDDEN))
                .orElse(
                    new SimpleErrorCollection(
                        String.format("Users %s has not permission to create issue", initiator),
                        ErrorCollection.Reason.FORBIDDEN)));
      }
    }
    return reporterJiraUser;
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

  private EventMessagesTextConverter.MarkdownFieldValueHolder getIssueDescription(
      ChatMessageEvent event, String template, @Nullable Issue issue) {
    return eventMessagesTextConverter
        .convertMessagesFromReplyAndForwardMessages(event::getMessageParts, issue, template)
        .orElse(
            new EventMessagesTextConverter.MarkdownFieldValueHolder(
                "Описание не заполнено", emptyList()));
  }

  private SummaryAndDescriptionStatusFromMainMessage getIssueSummary(
      ChatMessageEvent event,
      String template,
      User firstMessageReporter,
      User initiator,
      String tag) {

    String message = event.getMessage();

    String comment = replaceBotMentionAndCommand(message, tag);
    if (comment.length() != 0) {
      List<String> split = NEW_LINE_SPLITTER.splitToList(comment);
      String summary;
      if (split.size() != 0) {
        summary = split.get(0);
      } else {
        summary = comment;
      }
      if (!event.isHasForwards() && !event.isHasReply()) {
        return new SummaryAndDescriptionStatusFromMainMessage(summary, false);
      } else {
        return new SummaryAndDescriptionStatusFromMainMessage(summary, true);
      }
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

    return new SummaryAndDescriptionStatusFromMainMessage(result, true);
  }

  private String replaceBotMentionAndCommand(String message, String tag) {
    String botId = userChatService.getBotId();
    String botMention = String.format("@\\[%s\\]", botId);
    message = message.replaceAll(botMention, "").trim();
    return StringUtils.substringAfter(message, ISSUE_CREATION_BY_REPLY_PREFIX + tag).trim();
  }

  private String createFirstPartOfDescriptionByMainMessageText(
      final ChatMessageEvent event, final LinksInMessage linksInMessage, final String tag) {
    return Arrays.stream(
            replaceBotMentionAndCommand(
                    eventMessagesTextConverter.convertToJiraMarkdownStyleMainMessage(
                        event, linksInMessage),
                    tag)
                .split("\n"))
        .skip(1)
        .collect(Collectors.joining("\n"));
  }

  private String buildFullDescFromMainAndReplyMessages(
      final String descFromMainMessage, final String fromReplyMessage) {
    if (StringUtils.isBlank(descFromMainMessage)) {
      return fromReplyMessage;
    } else if (StringUtils.isBlank(fromReplyMessage)) {
      return descFromMainMessage;
    } else {
      return descFromMainMessage + "\n\n" + fromReplyMessage;
    }
  }

  @RequiredArgsConstructor
  private static class SummaryAndDescriptionStatusFromMainMessage {
    @NotNull private final String summary;
    private final boolean addDescFromParts;
  }
}
