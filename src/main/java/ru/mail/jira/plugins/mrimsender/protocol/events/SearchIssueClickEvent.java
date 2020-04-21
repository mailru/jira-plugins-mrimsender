package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;

@Getter
@Setter
public class SearchIssueClickEvent implements IcqButtonClickEvent {
    private final CallbackQueryEvent callbackQueryEvent;
    public SearchIssueClickEvent(CallbackQueryEvent callbackQueryEvent) {
        this.callbackQueryEvent = callbackQueryEvent;
    }
}
