/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.commands;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.MyteamRulesEngine;
import ru.mail.jira.plugins.myteam.rulesengine.RuleEventType;
import ru.mail.jira.plugins.myteam.rulesengine.service.IssueService;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;

@Rule(name = "hello message rule", description = "shows hello message")
public class DefaultMessageRule extends BaseCommandRule {

  static final String EVENT_TYPE = RuleEventType.DefaultMessage.toString();

  private final IssueService issueService;

  public DefaultMessageRule(UserChatService userChatService, IssueService issueService) {
    super(userChatService);
    this.issueService = issueService;
  }

  @Condition
  public boolean isValid(@Fact("command") String event) {
    return event.equals(EVENT_TYPE);
  }

  @Action
  public void execute(@Fact("event") ChatMessageEvent event)
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (user != null) {
      Locale locale = userChatService.getUserLocale(user);
      String chatId = event.getChatId();

      List<Forward> forwards = getForwardList(event);

      if (forwards != null) {
        Forward forward = forwards.get(0);
        String forwardMessageText = forward.getMessage().getText();
        URL issueUrl =
            Utils.tryFindUrlByPrefixInStr(forwardMessageText, issueService.getJiraBaseUrl());
        if (fireViewIssueResult(event, issueUrl)) return;
      }

      URL issueUrl =
          Utils.tryFindUrlByPrefixInStr(event.getMessage(), issueService.getJiraBaseUrl());

      if (fireViewIssueResult(event, issueUrl)) return;
      myteamClient.sendMessageText(
          chatId,
          i18nResolver.getRawText(
              locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.defaultMessage.text"),
          messageFormatter.getMenuButtons(user));
    }
  }

  private boolean fireViewIssueResult(@Fact("event") ChatMessageEvent event, URL issueUrl) {
    if (issueUrl != null) {
      String issueKey = StringUtils.substringAfterLast(issueUrl.getPath(), "/");
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
