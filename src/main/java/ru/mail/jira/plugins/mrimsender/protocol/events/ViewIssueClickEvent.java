package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;

@Getter
@Setter
public class ViewIssueClickEvent implements IcqButtonClickEvent {
    private final String issueKey;
    private final String userId;
    private final String chatId;
    private final String queryId;

    public ViewIssueClickEvent(CallbackQueryEvent callbackQueryEvent) {
        this.issueKey = StringUtils.substringAfter(callbackQueryEvent.getCallbackData(), "-");;
        this.userId = callbackQueryEvent.getFrom().getUserId();
        this.chatId = callbackQueryEvent.getMessage().getChat().getChatId();
        this.queryId = callbackQueryEvent.getQueryId();
    }
}
