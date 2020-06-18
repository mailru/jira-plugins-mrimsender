package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;

@Getter
public class ShowDefaultMessageEvent {
    private final String chatId;
    private final String userId;

    public ShowDefaultMessageEvent(ChatMessageEvent chatMessageEvent) {
        this.chatId = chatMessageEvent.getChatId();
        this.userId = chatMessageEvent.getUerId();
    }
}
