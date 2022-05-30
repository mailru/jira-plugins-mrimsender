/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.buttons;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.bot.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.rules.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.states.ViewingIssueState;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Rule(name = "search issue by key input", description = "Shows issue by key input")
public class SearchIssueByKeyInputRule extends BaseRule {

  static final RuleType NAME = ButtonRuleType.SearchIssueByKeyInput;

  public SearchIssueByKeyInputRule(UserChatService userChatService, RulesEngine rulesEngine) {
    super(userChatService, rulesEngine);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event)
      throws UserNotFoundException, MyteamServerErrorException, IOException {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    Locale locale = userChatService.getUserLocale(user);

    String message =
        userChatService.getRawText(
            locale,
            "ru.mail.jira.plugins.myteam.messageQueueProcessor.searchButton.insertIssueKey.message");

    userChatService.answerCallbackQuery(event.getQueryId());
    userChatService.sendMessageText(
        event.getChatId(), message, messageFormatter.getCancelButton(locale));

    ViewingIssueState newState = new ViewingIssueState(userChatService, null);
    newState.setWaiting(true);
    userChatService.setState(event.getChatId(), newState);
  }
}
