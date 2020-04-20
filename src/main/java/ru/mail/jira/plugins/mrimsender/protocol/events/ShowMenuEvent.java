package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Getter
@Setter
public class ShowMenuEvent {
    private NewMessageEvent newMessageEvent;
    public ShowMenuEvent(NewMessageEvent newMessageEvent) { this.newMessageEvent = newMessageEvent; }
}
