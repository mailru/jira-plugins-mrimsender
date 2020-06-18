package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;

@Getter
public class ShowMenuEvent {
    private final String userId;
    private final String chatId;

    public ShowMenuEvent(ChatMessageEvent chatMessageEvent) {
        this.userId = chatMessageEvent.getUerId();
        this.chatId = chatMessageEvent.getChatId();
    }
}
