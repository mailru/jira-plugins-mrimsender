/* (C)2022 */
package ru.mail.jira.plugins.myteam.rulesengine.states.base;

import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;

public interface RevertibleState {
  void revert(MyteamEvent event);
}
