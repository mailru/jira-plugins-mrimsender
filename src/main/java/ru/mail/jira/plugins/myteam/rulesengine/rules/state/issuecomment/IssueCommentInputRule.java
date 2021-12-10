/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.state.issuecomment;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import javax.naming.NoPermissionException;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.CommentingIssueState;

@Rule(
    name = "issue comment input result",
    description = "Fired when waiting for issue comment on input")
public class IssueCommentInputRule extends BaseRule {

  private final IssueService issueService;

  public IssueCommentInputRule(
      UserChatService userChatService, RulesEngine rulesEngine, IssueService issueService) {
    super(userChatService, rulesEngine);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("state") BotState state, @Fact("event") MyteamEvent event) {
    return state instanceof CommentingIssueState
        && event instanceof ChatMessageEvent
        && ((CommentingIssueState) state).getIssueKey().length() != 0;
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getChatId());

    CommentingIssueState state = (CommentingIssueState) userChatService.getState(event.getChatId());
    String issueKey = state.getIssueKey();
    if (user != null) {
      Locale locale = userChatService.getUserLocale(user);
      try {
        issueService.commentIssue(issueKey, user, event);
        userChatService.sendMessageText(
            event.getChatId(),
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.commentCreated"));
      } catch (NoPermissionException e) {
        userChatService.sendMessageText(
            event.getChatId(),
            userChatService.getRawText(
                locale,
                "ru.mail.jira.plugins.myteam.messageQueueProcessor.commentButton.noPermissions"));
      }
      userChatService.deleteState(event.getChatId());
    }
  }
}
