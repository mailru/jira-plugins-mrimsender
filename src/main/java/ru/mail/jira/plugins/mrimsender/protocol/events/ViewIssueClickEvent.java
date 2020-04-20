package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;

@Getter
@Setter
public class ViewIssueClickEvent implements IcqButtonClickEvent {
    public ViewIssueClickEvent(CallbackQueryEvent callbackQueryEvent) { this.callbackQueryEvent = callbackQueryEvent; }
    private final CallbackQueryEvent callbackQueryEvent;
}
