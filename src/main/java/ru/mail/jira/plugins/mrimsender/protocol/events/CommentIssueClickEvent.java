package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;

@Getter
@Setter
public class CommentIssueClickEvent implements IcqButtonClickEvent {
    private final CallbackQueryEvent callbackQueryEvent;
    public CommentIssueClickEvent(CallbackQueryEvent callbackQueryEvent) { this.callbackQueryEvent = callbackQueryEvent; }
}
