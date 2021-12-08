/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.rules.buttons;

import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Fact;
import org.jeasy.rules.annotation.Rule;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.BaseRule;
import ru.mail.jira.plugins.myteam.rulesengine.models.ButtonRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.service.UserChatService;
import ru.mail.jira.plugins.myteam.rulesengine.states.ViewingIssueState;

@Slf4j
@Rule(name = "search issue by key input", description = "Shows issue by key input")
public class SearchIssueByKeyInputRule extends BaseRule {

  static final ButtonRuleType NAME = ButtonRuleType.SearchIssueByKeyInput;

  public SearchIssueByKeyInputRule(UserChatService userChatService) {
    super(userChatService);
  }

  @Condition
  public boolean isValid(@Fact("command") String command) {
    return NAME.equalsName(command);
  }

  @Action
  public void execute(@Fact("event") ButtonClickEvent event) {
    ApplicationUser user = userChatService.getJiraUserFromUserChatId(event.getUserId());
    if (user != null) {
      Locale locale = userChatService.getUserLocale(user);
      String message =
          userChatService.getRawText(
              locale,
              "ru.mail.jira.plugins.myteam.messageQueueProcessor.searchButton.insertIssueKey.message");
      try {
        userChatService.answerCallbackQuery(event.getQueryId());
        userChatService.sendMessageText(
            event.getChatId(), message, messageFormatter.getCancelButton(locale));

        ViewingIssueState newState = new ViewingIssueState();
        newState.setWaiting(true);
        userChatService.setState(event.getChatId(), newState);
      } catch (MyteamServerErrorException | IOException e) {
        log.error(e.getLocalizedMessage());
      }
    }
  }
}
