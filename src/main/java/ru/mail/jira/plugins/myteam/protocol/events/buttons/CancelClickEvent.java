package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;

@Getter
public class CancelClickEvent {
    private final String queryId;
    private final String userId;
    private final String chatId;

    public CancelClickEvent(ButtonClickEvent chatButtonClickEvent) {
        this.queryId = chatButtonClickEvent.getQueryId();
        this.userId = chatButtonClickEvent.getUserId();
        this.chatId = chatButtonClickEvent.getChatId();
    }
}
