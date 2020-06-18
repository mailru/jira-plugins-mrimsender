package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.myteam.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.myteam.protocol.events.Event;

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
