package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;

@Getter
@Setter
public class SearchIssueClickEvent implements IcqButtonClickEvent {
    private final String queryId;
    private final String chatId;
    private final String userId;

    public SearchIssueClickEvent(CallbackQueryEvent callbackQueryEvent) {
        this.queryId = callbackQueryEvent.getQueryId();
        this.chatId = callbackQueryEvent.getMessage().getChat().getChatId();
        this.userId = callbackQueryEvent.getFrom().getUserId();
    }
}
