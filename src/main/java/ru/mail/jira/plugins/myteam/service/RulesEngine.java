/* (C)2021 */
package ru.mail.jira.plugins.myteam.service;

import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.ErrorRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.RuleType;

public interface RulesEngine {

  void fireCommand(RuleType command, MyteamEvent event);

  void fireCommand(RuleType command, MyteamEvent event, String args);

  void fireCommand(String command, MyteamEvent event, String args);

  void fireStateAction(MyteamEvent event, String args);

  void fireError(ErrorRuleType errorType, MyteamEvent event, Exception e);
}
