package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;

@Getter
public class ShowDefaultMessageEvent {
    private final String chatId;
    private final String userId;
    private final String message;

    public ShowDefaultMessageEvent(ChatMessageEvent chatMessageEvent) {
        chatId = chatMessageEvent.getChatId();
        userId = chatMessageEvent.getUerId();
        message = chatMessageEvent.getMessage();
    }
}
