package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;

@Getter
public class CreateIssueClickEvent {
    private final String queryId;
    private final String chatId;
    private final String userId;

    public CreateIssueClickEvent(ButtonClickEvent buttonClickEvent) {
        queryId = buttonClickEvent.getQueryId();
        chatId = buttonClickEvent.getChatId();
        userId = buttonClickEvent.getUserId();
    }
}
