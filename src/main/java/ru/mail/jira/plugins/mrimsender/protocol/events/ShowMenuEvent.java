package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Getter
public class ShowMenuEvent implements Event {
    private final String userId;
    private final String chatId;

    public ShowMenuEvent(ChatMessageEvent chatMessageEvent) {
        this.userId = chatMessageEvent.getUerId();
        this.chatId = chatMessageEvent.getChatId();
    }
}
