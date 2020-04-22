package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;

@Getter
@Setter
public class CancelClickEvent implements IcqButtonClickEvent {
    private final String queryId;
    private final String userId;
    private final String chatId;

    public CancelClickEvent(CallbackQueryEvent callbackQueryEvent) {
        this.queryId = callbackQueryEvent.getQueryId();
        this.userId = callbackQueryEvent.getFrom().getUserId();
        this.chatId = callbackQueryEvent.getMessage().getChat().getChatId();
    }
}
