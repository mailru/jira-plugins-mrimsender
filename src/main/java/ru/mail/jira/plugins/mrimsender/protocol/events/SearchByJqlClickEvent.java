package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;

@Getter
public class SearchByJqlClickEvent {
    private final String chatId;
    private final String userId;
    private final String queryId;

    public SearchByJqlClickEvent(ChatButtonClickEvent chatButtonClickEvent) {
        this.queryId = chatButtonClickEvent.getQueryId();
        this.userId = chatButtonClickEvent.getUserId();
        this.chatId = chatButtonClickEvent.getChatId();
    }

}
