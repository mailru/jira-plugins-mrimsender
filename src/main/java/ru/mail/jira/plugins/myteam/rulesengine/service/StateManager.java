/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import javax.annotation.Nullable;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;

public interface StateManager {

  void setState(String chatId, BotState state);

  @Nullable
  BotState getLastState(String chatId);

  @Nullable
  BotState getPrevState(String chatId);

  void deleteStates(String chatId);

  void deleteState(String chatId, BotState botState);
}
