package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Getter
@Setter
public class NewCommentMessageEvent {
    private final String userId;
    private final String chatId;
    private final String message;
    private final String commentingIssueKey;

    public NewCommentMessageEvent(NewMessageEvent newMessageEvent, String commentingIssueKey) {
        this.userId = newMessageEvent.getFrom().getUserId();
        this.chatId = newMessageEvent.getChat().getChatId();
        this.message = newMessageEvent.getText();
        this.commentingIssueKey = commentingIssueKey;
    }
}
