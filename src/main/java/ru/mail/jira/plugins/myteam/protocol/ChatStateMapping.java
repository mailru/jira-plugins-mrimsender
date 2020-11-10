/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import java.util.concurrent.ConcurrentHashMap;

public class ChatStateMapping {
  private final ConcurrentHashMap<String, ChatState> chatsStateMap = new ConcurrentHashMap<>();

  public ConcurrentHashMap<String, ChatState> getChatsStateMap() {
    return chatsStateMap;
  }
}
