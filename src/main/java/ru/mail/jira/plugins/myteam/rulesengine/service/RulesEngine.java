/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import org.jeasy.rules.api.Facts;
import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;
import ru.mail.jira.plugins.myteam.rulesengine.states.BotState;

public interface RulesEngine {

  void fireCommand(RuleType command, MyteamEvent event);

  void fireCommand(RuleType command, MyteamEvent event, String args);

  void fireCommand(String command, MyteamEvent event, String args);

  void fireStateAction(BotState state, MyteamEvent event, String args);

  void fireError(Facts facts);
}
