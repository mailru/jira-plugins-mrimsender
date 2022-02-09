/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.service;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.TimeZone;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.dto.IssueCreationSettingsDto;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.User;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Reply;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IssueCreationValidationException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.ProjectBannedException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueCreationService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.service.IssueCreationSettingsService;

@Rule(
    name = "Create issue by reply",
    description = "Create issue by reply if feature has been setup")
public class CreateIssueByReplyRule extends BaseRule {

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
      @Fact("args") String tag) {
    return isGroup
        && NAME.equalsName(command)
        && issueCreationSettingsService.hasChatSettings(event.getChatId(), tag)
        && event.getMessageParts().size() > 0;
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event, @Fact("args") String tag)
      throws UserNotFoundException, PermissionException, IssueCreationValidationException,
          ProjectBannedException, MyteamServerErrorException, IOException {

    IssueCreationSettingsDto settings =
        issueCreationSettingsService.getSettings(event.getChatId(), tag);

    if (settings == null || !issueCreationSettingsService.hasRequiredFields(settings)) {
      return;
    }

    User reporter = getReporterFromEventParts(event);

    ApplicationUser jiraUser =
        userChatService.getJiraUserFromUserChatId(
            reporter != null ? reporter.getUserId() : event.getUserId());

    HashMap<Field, String> fieldValues = new HashMap<>();

    fieldValues.put(
        issueCreationService.getField("summary"),
        String.format("Обращение от %s", jiraUser.getDisplayName()));
    fieldValues.put(issueCreationService.getField("description"), getIssueDescription(event));
    if (settings.getLabels() != null) {
      fieldValues.put(
          issueCreationService.getField("labels"), String.join(",", settings.getLabels()));
    }

    Issue issue =
        issueCreationService.createIssue(
            settings.getProjectKey(), settings.getIssueTypeId(), fieldValues, jiraUser);

    userChatService.sendMessageText(
        event.getChatId(),
        String.format(
            "Спасибо. По вашему обращению была создана задача:\n%s",
            messageFormatter.createIssueLink(issue.getKey())));
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
    StringBuilder str = new StringBuilder();
    event.getMessageParts().stream()
        .filter(part -> part instanceof Reply)
        .forEach(
            p -> {
              User user = ((Reply) p).getMessage().getFrom();

              LocalDateTime dateTime =
                  LocalDateTime.ofInstant(
                      Instant.ofEpochSecond(((Reply) p).getMessage().getTimestamp()),
                      TimeZone.getDefault().toZoneId());

              str.append("[").append(user.getFirstName()).append(" ");

              if (user.getLastName() != null) {
                str.append(" ").append(user.getLastName());
              }
              str.append("|").append(messageFormatter.getMyteamLink(user.getUserId())).append("] ");
              str.append("(").append(formatter.format(dateTime)).append("):\n").append("\n");
              str.append(((Reply) p).getPayload().getMessage().getText());
              str.append("\n\n");
            });
    return str.toString();
  }
}
