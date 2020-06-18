package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;

/**
 * As a result of this event handled user message on which "prev" button was clicked should be edited
 * and previous page of content should shown to user
 */
@Getter
public class PrevPageClickEvent {
    private final String chatId;
    private final long msgId;
    private final String userId;
    private final int currentPage;
    private final String queryId;

    public PrevPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage) {
        this.currentPage = currentPage;
        this.chatId = chatButtonClickEvent.getChatId();
        this.msgId = chatButtonClickEvent.getMsgId();
        this.userId = chatButtonClickEvent.getUserId();
        this.queryId = chatButtonClickEvent.getQueryId();
    }
}
