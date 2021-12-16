/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;

public interface RulesEngine {

  void fireCommand(RuleType command, @Nullable BotState state, MyteamEvent event);

  void fireCommand(RuleType command, @Nullable BotState state, MyteamEvent event, String args);

  void fireCommand(String command, @Nullable BotState state, MyteamEvent event, String args);

  void fireStateAction(
      @Nullable BotState state, @Nullable BotState prevState, MyteamEvent event, String args);

  void fireError(ErrorRuleType errorType, MyteamEvent event, String exceptionMessage);
}
