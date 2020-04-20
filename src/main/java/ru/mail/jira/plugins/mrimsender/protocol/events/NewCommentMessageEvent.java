package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Getter
@Setter
public class NewCommentMessageEvent {
    private final NewMessageEvent newMessageEvent;
    public NewCommentMessageEvent(NewMessageEvent newMessageEvent) { this.newMessageEvent = newMessageEvent; }
}
