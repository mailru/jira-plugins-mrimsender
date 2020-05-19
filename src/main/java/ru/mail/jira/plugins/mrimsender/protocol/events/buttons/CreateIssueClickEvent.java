package ru.mail.jira.plugins.mrimsender.protocol.events.buttons;

import lombok.Getter;
import ru.mail.jira.plugins.mrimsender.protocol.events.Event;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.ButtonClickEvent;

@Getter
public class CreateIssueClickEvent implements Event {
    private final String queryId;
    private final String chatId;
    private final String userId;

    public CreateIssueClickEvent(ButtonClickEvent buttonClickEvent) {
        queryId = buttonClickEvent.getQueryId();
        chatId = buttonClickEvent.getChatId();
        userId = buttonClickEvent.getUserId();
    }
}
