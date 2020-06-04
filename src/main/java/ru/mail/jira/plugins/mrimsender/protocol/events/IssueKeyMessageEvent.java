package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Getter
public class IssueKeyMessageEvent {
    private final String chatId;
    private final String issueKey;
    private final String userId;

    public IssueKeyMessageEvent(ChatMessageEvent chatMessageEvent) {
        this.chatId = chatMessageEvent.getChatId();
        this.issueKey = chatMessageEvent.getMessage().trim().toUpperCase();
        this.userId = chatMessageEvent.getUerId();
    }
}
