package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Getter
public class ChatMessageEvent implements Event {
    private final String chatId;
    private final String uerId;
    private final String message;

    public ChatMessageEvent(NewMessageEvent newMessageEvent) {
        this.chatId = newMessageEvent.getChat().getChatId();
        this.uerId = newMessageEvent.getFrom().getUserId();
        this.message = newMessageEvent.getText();
    }
}
