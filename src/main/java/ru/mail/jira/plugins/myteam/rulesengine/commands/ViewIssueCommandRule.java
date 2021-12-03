/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.commands;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.List;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.RuleEventType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Rule(name = "/menu command rule", description = "shows menu")
public class ViewIssueCommandRule extends BaseCommandRule {

  static final String NAME = RuleEventType.Issue.toString();
  private final IssueService issueService;

  public ViewIssueCommandRule(UserChatService userChatService, IssueService issueService) {
    super(userChatService);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return command.equals(NAME);
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event, @Fact("args") List<String> args)
      throws MyteamServerErrorException, IOException {
    if (args.size() > 1) {
      Issue issue =
          issueService.getIssueByUser(
              args.get(0), userChatService.getJiraUserFromUserChatId(event.getUserId()));
      ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
      if (user != null) {
        //        Locale locale = userChatService.getUserLocale(user);
        myteamClient.sendMessageText(
            event.getChatId(), issue.getSummary(), messageFormatter.getMenuButtons(user));
      }
    }
  }
}
