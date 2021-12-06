/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;

public interface StateManager {

  void setState(String chatId, BotState state);

  BotState getState(String chatId);

  void deleteState(String chatId);
}
