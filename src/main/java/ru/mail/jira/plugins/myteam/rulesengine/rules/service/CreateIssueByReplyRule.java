/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.service;

import static ru.mail.jira.plugins.myteam.commons.Const.ISSUE_CREATION_BY_REPLY_PREFIX;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.TimeZone;
import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.controller.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.AdminRulesRequiredException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.GroupAdminRule;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Rule(
    name = "Create issue by reply",
    description = "Create issue by reply if feature has been setup")
public class CreateIssueByReplyRule extends GroupAdminRule {

  static final RuleType NAME = CommandRuleType.CreateIssueByReply;

  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

  private final IssueCreationSettingsService issueCreationSettingsService;
  private final IssueCreationService issueCreationService;

  public CreateIssueByReplyRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      IssueCreationSettingsService issueCreationSettingsService,
      IssueCreationService issueCreationService) {
    super(userChatService, rulesEngine);
    this.issueCreationSettingsService = issueCreationSettingsService;
    this.issueCreationService = issueCreationService;
  }

  @Condition
  public boolean isValid(
      @Fact("command") String command,
      @Fact("event") ChatMessageEvent event,
      @Fact("isGroup") boolean isGroup,
      @Fact("args") String tag)
      throws AdminRulesRequiredException {
    checkAdminRules(event);
    return isGroup
        && NAME.equalsName(command)
        && issueCreationSettingsService.hasChatSettings(event.getChatId(), tag)
        && event.getMessageParts().size() > 0;
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event, @Fact("args") String tag)
      throws MyteamServerErrorException, IOException {
    try {

      IssueCreationSettingsDto settings =
          issueCreationSettingsService.getSettings(event.getChatId(), tag);

      if (settings == null || !issueCreationSettingsService.hasRequiredFields(settings)) {
        return;
      }

      User reporter = getReporterFromEventParts(event);

      ApplicationUser creator = userChatService.getJiraUserFromUserChatId(event.getUserId());
      if (creator == null || reporter == null) {
        return;
      }

      HashMap<Field, String> fieldValues = new HashMap<>();

      fieldValues.put(
          issueCreationService.getField(IssueFieldConstants.SUMMARY),
          getIssueSummary(
              event,
              String.format(
                  "%s %s",
                  reporter.getFirstName(),
                  reporter.getLastName() != null ? reporter.getLastName() : ""),
              tag));
      fieldValues.put(
          issueCreationService.getField(IssueFieldConstants.DESCRIPTION),
          getIssueDescription(event));
      if (settings.getLabels() != null) {
        fieldValues.put(
            issueCreationService.getField(IssueFieldConstants.LABELS),
            String.join(",", settings.getLabels()));
      }

      ApplicationUser reporterJiraUser;

      try {
        reporterJiraUser = userChatService.getJiraUserFromUserChatId(reporter.getUserId());
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

      userChatService.sendMessageText(
          event.getChatId(),
          String.format(
              "По вашему обращению была создана задача: %s",
              messageFormatter.createMarkdownIssueShortLink(issue.getKey())));
    } catch (Exception e) {
      userChatService.sendMessageText(
          event.getUserId(),
          String.format("Возникла ошибка при создании задачи.\n\n%s", e.getLocalizedMessage()));
    }
  }

  private User getReporterFromEventParts(ChatMessageEvent event) {
    for (Part part : event.getMessageParts()) {
      if (part instanceof Reply) {
        return ((Reply) part).getMessage().getFrom();
      }
    }
    return null;
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
