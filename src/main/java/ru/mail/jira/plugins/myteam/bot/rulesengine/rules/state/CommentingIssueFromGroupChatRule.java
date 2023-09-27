/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state;

import com.atlassian.jira.util.JiraKeyUtils;
import java.util.Map;
import kong.unirest.json.JSONObject;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.ChatAdminRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.CommentingIssueFromGroupChatState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(
    name = "Create comment after enter issue key in group chat",
    description = "Create comment after enter issue key in group chat")
public class CommentingIssueFromGroupChatRule extends ChatAdminRule {

  public CommentingIssueFromGroupChatRule(
      UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(
      @Fact("command") String command,
      @Fact("state") BotState state,
      @Fact("event") MyteamEvent event,
      @Fact("args") String arg) {
    return ButtonRuleType.CommentIssueByCommand.getName().equals(command)
        && state instanceof CommentingIssueFromGroupChatState
        && event instanceof ButtonClickEvent
        && JiraKeyUtils.isKeyInString(arg)
        && ((CommentingIssueFromGroupChatState) state).getChatMessageEvent() != null
        && event
            .getUserId()
            .equals(
                ((CommentingIssueFromGroupChatState) state)
                    .getChatMessageEvent()
                    .getFrom()
                    .getUserId());
  }

  @Action
  public void execute(@Fact("state") BotState state, @Fact("args") String issueKey) {
    if (state instanceof CommentingIssueFromGroupChatState) {
      final JSONObject jsonWithIssueKeyAndFlagToCheckByFiringRule =
          new JSONObject(Map.of("issueKey", issueKey));
      final String arg = jsonWithIssueKeyAndFlagToCheckByFiringRule.toString();
      ChatMessageEvent chatMessageEvent =
          ((CommentingIssueFromGroupChatState) state).getChatMessageEvent();
      userChatService.deleteState(chatMessageEvent.getChatId());
      rulesEngine.fireCommand(CommandRuleType.CommentIssueByMentionBot, chatMessageEvent, arg);
    }
  }
}
