package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Setter
@Getter
public class IssueKeyMessageEvent implements Event {
    private final String chatId;
    private final String message;
    private final String userId;

    public IssueKeyMessageEvent(NewMessageEvent newMessageEvent) {
        this.chatId = newMessageEvent.getChat().getChatId();
        this.message = newMessageEvent.getText().trim();
        this.userId = newMessageEvent.getFrom().getUserId();
    }
}
