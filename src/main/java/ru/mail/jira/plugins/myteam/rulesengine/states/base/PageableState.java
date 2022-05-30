/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.states.base;

import ru.mail.jira.plugins.myteam.bot.events.MyteamEvent;

public interface PageableState {

  void nextPage(MyteamEvent event);

  void prevPage(MyteamEvent event);

  void updatePage(MyteamEvent event, boolean editMessage);
}
