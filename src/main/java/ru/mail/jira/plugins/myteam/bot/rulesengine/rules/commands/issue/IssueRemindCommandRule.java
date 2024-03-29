/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jetbrains.annotations.NotNull;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.controller.dto.ReminderDto;
import ru.mail.jira.plugins.myteam.service.ReminderService;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@SuppressWarnings("UnusedVariable")
@Rule(name = "issue remind", description = "Remind issue by key, date and text")
public class IssueRemindCommandRule extends BaseRule {

  static final RuleType NAME = CommandRuleType.IssueRemind;
  private final ReminderService reminderService;

  public IssueRemindCommandRule(
      UserChatService userChatService, RulesEngine rulesEngine, ReminderService reminderService) {
    super(userChatService, rulesEngine);
    this.reminderService = reminderService;
  }

  @Condition
  public boolean isValid(
      @Fact("command") String command,
      @Fact("isGroup") boolean isGroup,
      @Fact("args") String args) {
    return NAME.equalsName(command) && !isGroup && args != null && args.length() > 0;
  }

  @Action
  public void execute(@Fact("event") final MyteamEvent event, @Fact("args") final String args)
      throws MyteamServerErrorException, IOException {
    final ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    final List<String> parsedArgs = RuleType.parseArgs(args);

    if (parsedArgs.size() < 1 || user == null) {
      rulesEngine.fireError(ErrorRuleType.UnknownError, event, "Issue remind args error");
      answerButtonCallback(event);
      return;
    }

    final ReminderDto reminderDto = buildReminderDtoFromEventData(event, parsedArgs);

    addReminderAndNotifyUser(event, reminderDto, user);
  }

  private void addReminderAndNotifyUser(
      final MyteamEvent event, final ReminderDto reminderDto, final ApplicationUser user)
      throws MyteamServerErrorException, IOException {
    try {
      reminderService.addReminder(reminderDto, user);

      userChatService.sendMessageText(
          event.getChatId(),
          userChatService.getRawText(
              "ru.mail.jira.plugins.myteam.mrimsenderEventListener.remindTomorrow.success.text"));

      answerButtonCallback(event);
    } catch (Exception e) {
      SentryClient.capture(e);
      userChatService.sendMessageText(
          event.getChatId(),
          String.format(
              "Adding reminder errors: %s",
              Utils.shieldText(StringUtils.defaultString(e.getMessage()))));
    }
  }

  @NotNull
  private static ReminderDto buildReminderDtoFromEventData(
      final MyteamEvent event, final List<String> parsedArgs) {
    final ReminderDto.ReminderDtoBuilder builder = ReminderDto.builder();

    builder.date(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));

    builder.issueKey(parsedArgs.get(0).toUpperCase());
    builder.userEmail(event.getUserId());

    if (parsedArgs.size() == 2) {
      builder.description(parsedArgs.get(1));
    }

    return builder.build();
  }
}
