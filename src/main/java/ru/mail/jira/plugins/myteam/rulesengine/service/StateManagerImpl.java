/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;

@Component
public class StateManagerImpl implements StateManager {

  private final Map<String, List<BotState>> statesMap = new HashMap<>();

  @Override
  public void setState(String chatId, BotState state) {
    List<BotState> states = getStates(chatId);
    if (states == null) {
      ArrayList<BotState> newStates = new ArrayList<>();
      newStates.add(state);
      statesMap.put(chatId, newStates);
    } else {
      states.add(state);
    }
  }

  @Override
  public BotState getLastState(String chatId) {
    List<BotState> states = getStates(chatId);

    if (states == null || states.size() == 0) {
      return null;
    }

    return states.get(states.size() - 1);
  }

  @Nullable
  @Override
  public BotState getPrevState(String chatId) {
    List<BotState> states = getStates(chatId);

    if (states == null || states.size() < 2) {
      return null;
    }

    return states.get(states.size() - 2);
  }

  @Override
  public void deleteStates(String chatId) {
    statesMap.remove(chatId);
  }

  @Override
  public void deleteState(String chatId, BotState botState) {
    List<BotState> states = getStates(chatId);
    if (states != null) states.remove(botState);
  }

  @Override
  public void setState(String chatId, BotState state, boolean deletePrevious) {
    if (deletePrevious) deleteStates(chatId);
    setState(chatId, state);
  }

  @Override
  public void revertState(String chatId) {
    @Nullable List<BotState> states = getStates(chatId);
    if (states == null || states.size() < 2) return;

    states.remove(states.size() - 1);
  }

  @Nullable
  private List<BotState> getStates(String chatId) {
    return statesMap.get(chatId);
  }
}
