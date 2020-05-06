package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ShowDefaultMessageEvent implements Event{
    private final String chatId;
    private final String userId;

    public ShowDefaultMessageEvent(ChatMessageEvent chatMessageEvent) {
        this.chatId = chatMessageEvent.getChatId();
        this.userId = chatMessageEvent.getUerId();
    }
}
