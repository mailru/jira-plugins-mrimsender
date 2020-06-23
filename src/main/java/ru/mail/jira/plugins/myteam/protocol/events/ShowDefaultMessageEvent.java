package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.protocol.events.buttons.ButtonClickEvent;

@Getter
public class ShowDefaultMessageEvent {
    private final String chatId;
    private final String userId;

    public ShowDefaultMessageEvent(ChatMessageEvent chatMessageEvent) {
        this.chatId = chatMessageEvent.getChatId();
        this.userId = chatMessageEvent.getUerId();
    }

    public ShowDefaultMessageEvent(ButtonClickEvent buttonClickEvent) {
        this.chatId = buttonClickEvent.getChatId();
        this.userId = buttonClickEvent.getUserId();
    }
}
