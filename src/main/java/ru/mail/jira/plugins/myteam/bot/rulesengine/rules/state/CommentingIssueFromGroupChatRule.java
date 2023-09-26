/* (C)2023 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.state;

import com.atlassian.jira.util.JiraKeyUtils;
import java.util.Map;
import kong.unirest.json.JSONObject;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.ChatAdminRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.CommentingIssueFromGroupChatState;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(
    name = "Create comment after enter issue key in group chat",
    description = "Create comment after enter issue key in group chat")
public class CommentingIssueFromGroupChatRule extends ChatAdminRule {

  private final MyteamApiClient myteamApiClient;

  public CommentingIssueFromGroupChatRule(
      UserChatService userChatService, RulesEngine rulesEngine, MyteamApiClient myteamApiClient) {
    super(userChatService, rulesEngine);
    this.myteamApiClient = myteamApiClient;
  }

  @Condition
  public boolean isValid(@Fact("state") BotState state, @Fact("event") MyteamEvent event) {
    return state instanceof CommentingIssueFromGroupChatState
        && event instanceof ChatMessageEvent
        && JiraKeyUtils.isKeyInString(((ChatMessageEvent) event).getMessage().toUpperCase())
        && ((CommentingIssueFromGroupChatState) state).getChatMessageEvent() != null
        && ((ChatMessageEvent) event)
            .getFrom()
            .getUserId()
            .equals(
                ((CommentingIssueFromGroupChatState) state)
                    .getChatMessageEvent()
                    .getFrom()
                    .getUserId());
  }

  @Action
  public void execute(@Fact("state") BotState state, @Fact("event") ChatMessageEvent event) {
    if (state instanceof CommentingIssueFromGroupChatState) {
      final String messageWithoutMentionBot =
          event.getMessage()
                  .replaceAll(String.format("@\\[%s\\]", myteamApiClient.getBotId()), "")
                  .trim();
      final String issueKey =
          JiraKeyUtils.getIssueKeysFromString(messageWithoutMentionBot.toUpperCase()).stream()
              .findFirst()
              .orElse("");
      final JSONObject jsonWithIssueKeyAndFlagToCheckByFiringRule =
          new JSONObject(Map.of("issueKey", issueKey, "issueKeyReceivedFromNewEvent", true));
      final String arg = jsonWithIssueKeyAndFlagToCheckByFiringRule.toString();
      ChatMessageEvent chatMessageEvent =
          ((CommentingIssueFromGroupChatState) state).getChatMessageEvent();
      userChatService.deleteState(chatMessageEvent.getChatId());
      rulesEngine.fireCommand(CommandRuleType.CommentIssueByMentionBot, chatMessageEvent, arg);
    }
  }
}
