/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states;

import ru.mail.jira.plugins.myteam.protocol.events.MyteamEvent;

public interface CancelableState {

  void cancel(MyteamEvent event);
}
