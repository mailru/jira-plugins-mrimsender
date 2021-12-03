/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.commands;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.MyteamRulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.RuleEventType;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Rule(name = "hello message rule", description = "shows hello message")
public class DefaultMessageRule extends BaseCommandRule {

  static final RuleEventType NAME = RuleEventType.DefaultMessage;

  public DefaultMessageRule(UserChatService userChatService) {
    super(userChatService);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") MyteamEvent event)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (user != null && event instanceof ChatMessageEvent) {
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
  }

  private boolean fireViewIssueResult(@Fact("event") MyteamEvent event, String issueKey) {
    if (issueKey != null) {
      userChatService.fireRule(
          MyteamRulesEngine.formCommandFacts(
              RuleEventType.Issue.toString(), event, Collections.singletonList(issueKey)));
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
