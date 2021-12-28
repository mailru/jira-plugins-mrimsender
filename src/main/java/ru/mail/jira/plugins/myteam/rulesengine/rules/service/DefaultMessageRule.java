/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.service;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.RulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;

@Rule(
    name = "Default message",
    description = "Shows issue if message contains Issue key otherwise shows menu")
public class DefaultMessageRule extends BaseRule {

  public DefaultMessageRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("state") BotState state, @Fact("event") MyteamEvent event) {
    return !state.isWaiting()
        && event.getChatType() != ChatType.GROUP
        && event instanceof ChatMessageEvent;
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event)
      throws MyteamServerErrorException, IOException, UserNotFoundException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    Locale locale = userChatService.getUserLocale(user);

    String chatId = event.getChatId();

    List<Forward> forwards = getForwardList((ChatMessageEvent) event);

    if (forwards != null) {
      Forward forward = forwards.get(0);
      String forwardMessageText = forward.getMessage().getText();
      String issueKey = Utils.findIssueKeyInStr(forwardMessageText);
      if (fireViewIssueResult(event, issueKey)) return;
    }

    String issueKey = Utils.findIssueKeyInStr(((ChatMessageEvent) event).getMessage());
    if (fireViewIssueResult(event, issueKey)) return;
    userChatService.sendMessageText(
        chatId,
        userChatService.getRawText(
            locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.defaultMessage.text"),
        messageFormatter.getMenuButtons(user));
  }

  private boolean fireViewIssueResult(MyteamEvent event, String issueKey) {
    if (issueKey != null) {
      rulesEngine.fireCommand(CommandRuleType.Issue.getName(), event, issueKey);
      return true;
    }
    return false;
  }

  private List<Forward> getForwardList(ChatMessageEvent event) {
    return event.isHasForwards()
        ? event.getMessageParts().stream()
            .filter(part -> part instanceof Forward)
            .map(part -> (Forward) part)
            .collect(Collectors.toList())
        : null;
  }
}
