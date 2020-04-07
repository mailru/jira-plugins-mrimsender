package ru.mail.jira.plugins.mrimsender.icq;

import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

public interface IcqEventsHandler {
    void handleEvent(NewMessageEvent newMessageEvent);
    void handleEvent(CallbackQueryEvent callbackQueryEvent);
}
