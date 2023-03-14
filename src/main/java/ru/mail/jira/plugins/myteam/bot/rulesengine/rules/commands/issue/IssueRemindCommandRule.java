/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
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
  public void execute(@Fact("event") MyteamEvent event, @Fact("args") String args)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    List<String> parsedArgs = RuleType.parseArgs(args);

    rulesEngine.fireError(ErrorRuleType.UnknownError, event);

    if (parsedArgs.size() < 2 || user == null) {
      rulesEngine.fireError(ErrorRuleType.UnknownError, event);
      answerButtonCallback(event);
    }

    ReminderDto.ReminderDtoBuilder builder = ReminderDto.builder();

    try {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      builder.date(format.parse(parsedArgs.get(1)));
    } catch (ParseException e) {
      rulesEngine.fireError(ErrorRuleType.UnknownError, event, e);
      answerButtonCallback(event);
    }

    builder.issueKey(parsedArgs.get(0));
    builder.userEmail(event.getUserId());

    if (parsedArgs.size() == 3) {
      builder.description(parsedArgs.get(2));
    }

    reminderService.addReminder(builder.build(), user);

    userChatService.sendMessageText(
        event.getChatId(),
        userChatService.getRawText(
            "ru.mail.jira.plugins.myteam.mrimsenderEventListener.remindTomorrow.success.text"));

    answerButtonCallback(event);
  }
}
