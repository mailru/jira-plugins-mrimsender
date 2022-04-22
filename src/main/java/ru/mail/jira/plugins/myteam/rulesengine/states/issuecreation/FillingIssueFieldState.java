/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation;

import com.atlassian.jira.issue.fields.Field;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
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

  @Getter
  private final Field field;
  @Getter
  @Setter
  private String value;

  public FillingIssueFieldState(UserChatService userChatService, RulesEngine rulesEngine, Field field) {
    this.userChatService = userChatService;
    this.rulesEngine = rulesEngine;
    this.field = field;
    isWaiting = true;
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
  }

  @Override
  public void prevPage(MyteamEvent event) {
  }

  @Override
  public void updatePage(MyteamEvent event, boolean editMessage) {
    rulesEngine.fireCommand(StateActionRuleType.ShowCreatingIssueProgressMessage, event);
  }
}
