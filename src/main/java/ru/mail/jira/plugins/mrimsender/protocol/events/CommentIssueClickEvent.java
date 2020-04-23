package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;

@Getter
@Setter
public class CommentIssueClickEvent implements Event {
    private final String userId;
    private final String chatId;
    private final String queryId;
    private final String issueKey;

    public CommentIssueClickEvent(CallbackQueryEvent callbackQueryEvent) {
        this.userId = callbackQueryEvent.getFrom().getUserId();
        this.chatId = callbackQueryEvent.getMessage().getChat().getChatId();
        this.queryId = callbackQueryEvent.getQueryId();
        this.issueKey = StringUtils.substringAfter(callbackQueryEvent.getCallbackData(), "-");
    }
}
