package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Setter
@Getter
public class NewIssueKeyMessageEvent {
    public NewIssueKeyMessageEvent(NewMessageEvent newMessageEvent) { this.newMessageEvent = newMessageEvent; }
    private final NewMessageEvent newMessageEvent;
}
