package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import ru.mail.jira.plugins.mrimsender.icq.dto.ChatType;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;

@Getter
public class ButtonClickEvent implements Event {
    private final String queryId;
    private final String chatId;
    private final String userId;
    private final long msgId;
    private final String callbackData;
    private final ChatType chatType;

    public ButtonClickEvent(CallbackQueryEvent callbackQueryEvent) {
        this.queryId = callbackQueryEvent.getQueryId();
        this.chatId = callbackQueryEvent.getMessage().getChat().getChatId();
        this.userId = callbackQueryEvent.getFrom().getUserId();
        this.msgId = callbackQueryEvent.getMessage().getMsgId();
        this.callbackData = callbackQueryEvent.getCallbackData();
        this.chatType = ChatType.fromApiValue(callbackQueryEvent.getMessage().getChat().getType());
    }
}
