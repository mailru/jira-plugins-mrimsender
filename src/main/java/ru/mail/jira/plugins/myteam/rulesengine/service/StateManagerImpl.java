/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;

@Component
public class StateManagerImpl implements StateManager {

  private final Map<String, BotState> states;

  public StateManagerImpl() {
    states = new HashMap<>();
  }

  @Override
  public void setState(String chatId, BotState state) {
    states.put(chatId, state);
  }

  @Override
  public BotState getState(String chatId) {
    return states.get(chatId);
  }

  @Override
  public void deleteState(String chatId) {
    states.remove(chatId);
  }
}
