/* (C)2021 */
package ru.mail.jira.plugins.myteam.bot.rulesengine.rules.service;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.bot.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(
    name = "Default message",
    description = "Shows issue if message contains Issue key otherwise shows menu")
public class DefaultMessageRule extends BaseRule {

  private static final Pattern pattern =
      Pattern.compile("[A-Z][A-Z\\d]+-[\\d]+", Pattern.CASE_INSENSITIVE);

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
      throws MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    Locale locale = userChatService.getUserLocale(user);

    String chatId = event.getChatId();

    List<Forward> forwards = getForwardList((ChatMessageEvent) event);

    if (forwards != null) {
      Forward forward = forwards.get(0);
      String forwardMessageText = forward.getMessage().getText();
      String issueKey = findIssueKeyInStr(forwardMessageText);
      if (fireViewIssueResult(event, issueKey)) return;
    }

    String issueKey = findIssueKeyInStr(((ChatMessageEvent) event).getMessage());
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

  @Nullable
  private static String findIssueKeyInStr(@Nullable String str) {
    if (str == null) {
      return null;
    }
    Matcher result = pattern.matcher(str);
    return result.find() ? result.group(0) : null;
  }
}
