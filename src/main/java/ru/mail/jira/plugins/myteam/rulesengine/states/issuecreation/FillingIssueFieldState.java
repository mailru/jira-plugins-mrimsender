/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation;

import com.atlassian.jira.issue.fields.Field;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.protocol.events.ButtonClickEvent;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.core.Pager;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.CancelableState;
import ru.mail.jira.plugins.myteam.rulesengine.states.base.PageableState;
import ru.mail.jira.plugins.myteam.service.RulesEngine;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Slf4j
public class FillingIssueFieldState extends BotState implements CancelableState, PageableState {

  private final UserChatService userChatService;
  private final RulesEngine rulesEngine;

  @Getter private final Field field;
  @Getter private final boolean isSearchOn;
  @Getter private final boolean isAdditional;
  @Getter @Setter private String input;
  @Getter @Setter private String value;
  @Getter private final Pager pager;

  public FillingIssueFieldState(
      UserChatService userChatService, RulesEngine rulesEngine, Field field) {
    this.userChatService = userChatService;
    this.rulesEngine = rulesEngine;
    this.field = field;
    this.isSearchOn = false;
    this.isAdditional = false;
    isWaiting = true;
    pager = new Pager(0, 10);
  }

  public FillingIssueFieldState(
      UserChatService userChatService,
      RulesEngine rulesEngine,
      Field field,
      boolean isSearchOn,
      boolean isAdditional) {
    this.userChatService = userChatService;
    this.rulesEngine = rulesEngine;
    this.field = field;
    this.isSearchOn = isSearchOn;
    this.isAdditional = isAdditional;
    isWaiting = true;
    pager = new Pager(0, 2);
  }

  @Override
  public UserChatService getUserChatService() {
    return userChatService;
  }

  @Override
  public void onError(Exception e) {
    log.error(e.getLocalizedMessage(), e);
  }

  @Override
  public void nextPage(MyteamEvent event) {
    pager.nextPage();
    updatePage(event, true);
  }

  @Override
  public void prevPage(MyteamEvent event) {
    pager.prevPage();
    updatePage(event, true);
  }

  @Override
  public void updatePage(MyteamEvent event, boolean editMessage) {
    rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
  }

  @Override
  public void cancel(MyteamEvent event) {
    userChatService.revertState(event.getChatId());
    rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
    if (event instanceof ButtonClickEvent) {
      try {
        userChatService.answerCallbackQuery(((ButtonClickEvent) event).getQueryId());
      } catch (MyteamServerErrorException e) {
        log.error(e.getLocalizedMessage(), e);
      }
    }
  }
}
