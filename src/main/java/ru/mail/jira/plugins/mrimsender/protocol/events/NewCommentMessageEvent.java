package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Getter
public class NewCommentMessageEvent implements Event {
    private final String userId;
    private final String chatId;
    private final String message;
    private final String commentingIssueKey;

    public NewCommentMessageEvent(ChatMessageEvent chatMessageEvent, String commentingIssueKey) {
        this.userId = chatMessageEvent.getUerId();
        this.chatId = chatMessageEvent.getChatId();
        this.message = chatMessageEvent.getMessage();
        this.commentingIssueKey = commentingIssueKey;
    }
}
