/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.service;

import static ru.mail.jira.plugins.myteam.commons.Const.ISSUE_CREATION_BY_REPLY_PREFIX;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.GroupAdminRule;
import ru.mail.jira.plugins.myteam.service.*;

@Slf4j
@Rule(
    name = "Create issue by reply",
    description = "Create issue by reply if feature has been setup")
public class CreateIssueByReplyRule extends GroupAdminRule {

  static final RuleType NAME = CommandRuleType.CreateIssueByReply;

  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  private final IssueCreationSettingsService issueCreationSettingsService;
  private final IssueCreationService issueCreationService;
  private final IssueService issueService;

  public CreateIssueByReplyRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueCreationSettingsService issueCreationSettingsService,
      IssueCreationService issueCreationService,
      IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueCreationSettingsService = issueCreationSettingsService;
    this.issueCreationService = issueCreationService;
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(
      @Fact("command") String command,
      @Fact("event") ChatMessageEvent event,
      @Fact("isGroup") boolean isGroup,
      @Fact("args") String tag)
      throws AdminRulesRequiredException {
    log.error(
        MessageFormatter.formLogMessage(
            "isValid",
            "START Validate",
            ImmutableMap.of(
                "command",
                command,
                "chatId",
                event.getChatId(),
                "tag",
                tag,
                "isGroup",
                String.valueOf(isGroup))));
    checkAdminRules(event);

    log.error(MessageFormatter.formLogMessage("isValid", "ADMIN RULES VALIDATED"));

    boolean hasSettings = issueCreationSettingsService.hasChatSettings(event.getChatId(), tag);

    log.error(
        MessageFormatter.formLogMessage(
            "isValid - issueCreationSettingsService.hasChatSettings",
            "Check in cache",
            ImmutableMap.of("hasSettings", String.valueOf(hasSettings))));

    boolean result = isGroup && NAME.equalsName(command) && event.isHasReply() && hasSettings;

    log.error(
        MessageFormatter.formLogMessage(
            "isValid - result", "result", ImmutableMap.of("result", String.valueOf(result))));

    return result;
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event, @Fact("args") String tag)
      throws MyteamServerErrorException, IOException {
    try {

      log.error(MessageFormatter.formLogMessage("execute", "START execute"));

      IssueCreationSettingsDto settings =
          issueCreationSettingsService.getSettings(event.getChatId(), tag);

      if (settings == null) {
        log.error(
            MessageFormatter.formLogMessage(
                "execute", " issueCreationSettingsService.getSettings returned null"));
      } else {
        log.error(
            MessageFormatter.formLogMessage(
                "execute",
                "Got settings from cache",
                ImmutableMap.of(
                    "id",
                    String.valueOf(settings.getId()),
                    "enabled",
                    String.valueOf(settings.getEnabled()),
                    "tag",
                    settings.getTag(),
                    "project",
                    settings.getProjectKey(),
                    "issueType",
                    settings.getIssueTypeId())));
      }

      if (settings == null || !issueCreationSettingsService.hasRequiredFields(settings)) {
        return;
      }

      List<User> reporters = getReportersFromEventParts(event);

      User firstMessageReporter = reporters.get(0);

      ApplicationUser creator = userChatService.getJiraUserFromUserChatId(event.getUserId());

      if (creator == null || firstMessageReporter == null) {
        return;
      }

      HashMap<Field, String> fieldValues = new HashMap<>();

      String firstReporterDisplayName =
          String.format(
              "%s %s",
              firstMessageReporter.getFirstName(),
              firstMessageReporter.getLastName() != null ? firstMessageReporter.getLastName() : "");

      fieldValues.put(
          issueCreationService.getField(IssueFieldConstants.SUMMARY),
          getIssueSummary(event, firstReporterDisplayName, tag));
      fieldValues.put(
          issueCreationService.getField(IssueFieldConstants.DESCRIPTION),
          getIssueDescription(event));

      if (settings.getLabels() != null) {
        fieldValues.put(
            issueCreationService.getField(IssueFieldConstants.LABELS),
            String.join(",", settings.getLabels()));
      }

      ApplicationUser reporterJiraUser;

      log.error(
          MessageFormatter.formLogMessage(
              "execute",
              "Filled fields for issue",
              ImmutableMap.of("fieldsCount", String.valueOf(fieldValues.size()))));

      try {
        reporterJiraUser =
            userChatService.getJiraUserFromUserChatId(firstMessageReporter.getUserId());
      } catch (UserNotFoundException e) {
        reporterJiraUser = creator;
      }

      Issue issue =
          issueCreationService.createIssue(
              settings.getProjectKey(),
              settings.getIssueTypeId(),
              fieldValues,
              creator,
              reporterJiraUser);

      reporters.stream() // add watchers
          .map(
              u -> {
                try {
                  return userChatService.getJiraUserFromUserChatId(u.getUserId());
                } catch (UserNotFoundException e) {
                  return null;
                }
              })
          .filter(Objects::nonNull)
          .forEach(user -> issueService.watchIssue(issue, user));

      userChatService.sendMessageText(
          event.getChatId(),
          String.format(
              "По вашему обращению была создана задача: %s",
              messageFormatter.createMarkdownIssueShortLink(issue.getKey())));

      log.error(MessageFormatter.formLogMessage("execute", "Issue has been crated"));
    } catch (Exception e) {
      log.error(MessageFormatter.formLogMessage("execute", "Error while creating Issue"));

      log.error(e.getLocalizedMessage(), e);
      SentryClient.capture(e);
      userChatService.sendMessageText(
          event.getUserId(),
          String.format("Возникла ошибка при создании задачи.\n\n%s", e.getLocalizedMessage()));
    }
  }

  private List<User> getReportersFromEventParts(ChatMessageEvent event) {
    return event.getMessageParts().stream()
        .filter(p -> p instanceof Reply)
        .map(p -> ((Reply) p).getMessage().getFrom())
        .collect(Collectors.toList());
  }

  private String getIssueDescription(ChatMessageEvent event) {
    StringBuilder builder = new StringBuilder();

    event.getMessageParts().stream()
        .filter(part -> part instanceof Reply)
        .forEach(
            p -> {
              User user = ((Reply) p).getMessage().getFrom();

              LocalDateTime dateTime =
                  LocalDateTime.ofInstant(
                      Instant.ofEpochSecond(((Reply) p).getMessage().getTimestamp()),
                      TimeZone.getDefault().toZoneId());

              builder.append(messageFormatter.formatMyteamUserLink(user));
              builder
                  .append("(")
                  .append(formatter.format(dateTime))
                  .append("):\n")
                  .append("\n{quote}");
              builder.append(((Reply) p).getPayload().getMessage().getText());
              builder.append("{quote}\n\n");
            });
    return builder.toString();
  }

  private String getIssueSummary(ChatMessageEvent event, String userName, String tag) {

    String comment =
        StringUtils.substringAfter(event.getMessage(), ISSUE_CREATION_BY_REPLY_PREFIX + tag).trim();
    if (comment.length() != 0) {
      return comment;
    }
    return String.format("Обращение от %s", userName);
  }
}
