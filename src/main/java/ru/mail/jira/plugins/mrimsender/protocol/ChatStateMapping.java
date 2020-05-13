package ru.mail.jira.plugins.mrimsender.protocol;

import java.util.concurrent.ConcurrentHashMap;

public class ChatStateMapping {
    private final ConcurrentHashMap<String, ChatState> chatsStateMap = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, ChatState> getChatsStateMap() {
        return chatsStateMap;
    }
}
