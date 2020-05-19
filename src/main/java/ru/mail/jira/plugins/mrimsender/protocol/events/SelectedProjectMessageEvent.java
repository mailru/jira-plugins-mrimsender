package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;

@Getter
public class SelectedProjectMessageEvent {
    private final String userId;
    private final String chatId;
    private final String selectedProjectKey;

    public SelectedProjectMessageEvent(ChatMessageEvent chatMessageEvent) {
        userId = chatMessageEvent.getUerId();
        chatId = chatMessageEvent.getChatId();
        selectedProjectKey = chatMessageEvent.getMessage().trim().toUpperCase();
    }
}
