package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Getter
@Setter
public class ShowMenuEvent implements Event {
    private final String userId;
    private final String chatId;

    public ShowMenuEvent(NewMessageEvent newMessageEvent) {
        this.userId = newMessageEvent.getFrom().getUserId();
        this.chatId = newMessageEvent.getChat().getChatId();
    }
}
