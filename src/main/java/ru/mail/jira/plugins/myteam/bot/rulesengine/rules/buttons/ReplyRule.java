/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.buttons;

import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestConfigurationDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.UserFieldDto;
import ru.mail.jira.plugins.myteam.accessrequest.model.AccessRequestConfigurationRepository;
import ru.mail.jira.plugins.myteam.accessrequest.service.AccessRequestService;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "reply", description = "reply user access")
public class ReplyRule extends BaseRule {
  private enum ReplyArgs {
    COMMAND_ARG,
    HISTORY_ARG,
    USERKEY_ARG,
  }

  public enum ReplyCommands {
    COMMAND_ALLOW,
    COMMAND_FORBID
  }

  static final ButtonRuleType NAME = ButtonRuleType.AccessReply;
  private final AccessRequestService accessRequestService;
  private final IssueService issueService;

  public ReplyRule(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      AccessRequestService accessRequestService,
      IssueService issueService) {
    super(userChatService, rulesEngine);
    this.accessRequestService = accessRequestService;
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event, @Fact("args") String args)
      throws MyteamServerErrorException, IOException {

    List<String> parsedArgs = RuleType.parseArgs(args);
    ReplyCommands replyCommand =
        ReplyCommands.valueOf(parsedArgs.get(ReplyArgs.COMMAND_ARG.ordinal()));
    int historyId = Integer.parseInt(parsedArgs.get(ReplyArgs.HISTORY_ARG.ordinal()));
    String userKey = parsedArgs.get(ReplyArgs.USERKEY_ARG.ordinal());

    AccessRequestDto accessRequestDto = accessRequestService.getAccessRequestHistoryDto(historyId);
    if (accessRequestDto == null) return;

    try {
      MutableIssue mutableIssue =
          issueService.getMutableIssue(Objects.requireNonNull(accessRequestDto.getIssueId()));
      AccessRequestConfigurationDto accessRequestConfigurationDto =
          accessRequestService.getAccessRequestConfigurationDto(
              Objects.requireNonNull(mutableIssue.getProjectId()));
      ApplicationUser requester =
          accessRequestService.getAccessUserByKey(
              Objects.requireNonNull(accessRequestDto.getRequesterKey()));
      ApplicationUser responder = accessRequestService.getAccessUserByKey(userKey);

      String message = "";
      Boolean replyStatus = accessRequestDto.getReplyStatus();
      if (replyStatus == null) {
        // Access Request is not processed
        updateAccessRequest(accessRequestDto, replyCommand, userKey, historyId);
        if (replyCommand.equals(ReplyCommands.COMMAND_ALLOW))
          for (UserFieldDto permissionField :
              Objects.requireNonNull(accessRequestConfigurationDto.getAccessPermissionFields()))
            updateAccessIssueField(requester, permissionField, mutableIssue);
        message = messageFormatter.formatAccessReplyMessage(requester, mutableIssue, replyCommand);
      } else {
        // Access Request already processed
        message =
            messageFormatter.formatProcessedReplyMessage(
                responder, requester, mutableIssue, replyStatus);
      }
      userChatService.editMessageText(event.getChatId(), event.getMsgId(), message, null);

      // Notify other admins and User who sent the request
      if (replyStatus == null) {
        String newsletterMessage =
            messageFormatter.formatProcessedNewsletterMessage(
                responder,
                requester,
                mutableIssue,
                replyCommand.equals(ReplyCommands.COMMAND_ALLOW));
        accessRequestService.notifyAllAdminsReply(
            accessRequestDto, responder, mutableIssue, newsletterMessage);
        userChatService.sendMessageText(
            requester.getEmailAddress(),
            messageFormatter.formatAccessReplyRequesterMessage(
                requester, mutableIssue, replyCommand));
      }
    } catch (Exception e) {
      userChatService.sendMessageText(
          event.getChatId(), messageFormatter.formatAccessReplyError(e));
      SentryClient.capture(e, Map.of("user", event.getUserId()));
    }
    userChatService.answerCallbackQuery(event.getQueryId());
  }

  private void updateAccessRequest(
      AccessRequestDto accessRequestDto,
      ReplyCommands replyCommand,
      String userKey,
      int historyId) {
    UserDto userDto =
        accessRequestDto.getUsers().stream()
            .filter(user -> user.getUserKey().equals(userKey))
            .findAny()
            .orElse(null);

    if (userDto != null) {
      accessRequestDto.setReplyAdmin(userDto);
      accessRequestDto.setReplyDate(Utils.convertToDate(LocalDateTime.now(ZoneId.systemDefault())));
      accessRequestDto.setReplyStatus(replyCommand.equals(ReplyCommands.COMMAND_ALLOW));
    }
    accessRequestService.updateAccessHistory(historyId, accessRequestDto);
  }

  private void updateAccessIssueField(
      ApplicationUser requester, UserFieldDto userFieldDto, MutableIssue mutableIssue)
      throws Exception {
    switch (userFieldDto.getId()) {
      case AccessRequestConfigurationRepository.ASSIGNEE:
        issueService.setAssigneeIssue(mutableIssue, requester);
        break;
      case AccessRequestConfigurationRepository.REPORTER:
        issueService.setReporterIssue(mutableIssue, requester);
        break;
      case AccessRequestConfigurationRepository.WATCHERS:
        issueService.watchIssue(mutableIssue, requester);
        break;
      default:
        throw new Exception("User field not recognized");
    }
  }
}
