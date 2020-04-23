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

    public IssueKeyMessageEvent(ChatMessageEvent chatMessageEvent) {
        this.chatId = chatMessageEvent.getChatId();
        this.message = chatMessageEvent.getMessage().trim();
        this.userId = chatMessageEvent.getUerId();
    }
}
