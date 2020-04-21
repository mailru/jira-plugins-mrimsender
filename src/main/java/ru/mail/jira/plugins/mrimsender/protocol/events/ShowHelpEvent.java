package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Getter
@Setter
public class ShowHelpEvent {
    private NewMessageEvent newMessageEvent;
    public ShowHelpEvent(NewMessageEvent newMessageEvent) { this.newMessageEvent = newMessageEvent; }
}
