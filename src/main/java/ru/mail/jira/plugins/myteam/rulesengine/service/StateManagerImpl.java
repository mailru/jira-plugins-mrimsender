/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;
import ru.mail.jira.plugins.myteam.rulesengine.states.EmptyState;

@Component
public class StateManagerImpl implements StateManager {

  private final Map<String, BotState> states = new HashMap<>();

  @Override
  public void setState(String chatId, BotState state) {
    states.put(chatId, state);
  }

  @Override
  @NotNull
  public BotState getState(String chatId) {
    BotState state = states.get(chatId);
    return state == null
        ? new EmptyState()
        : state; // as we use state as rules engine Facts ==> Fact cannot be nullable
  }

  @Override
  public void deleteState(String chatId) {
    states.remove(chatId);
  }
}
