package ru.mail.jira.plugins.mrimsender.protocol.events.buttons;

import lombok.Getter;
import ru.mail.jira.plugins.mrimsender.protocol.events.Event;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.ButtonClickEvent;

/**
 * As a result of this event handled user message on which "next" button was clicked should be edited
 * and next page of content should shown to user
 */
@Getter
public class NextPageClickEvent implements Event {
    private final String chatId;
    private final long msgId;
    private final String userId;
    private final String queryId;
    private final int currentPage;

    public NextPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage) {
        this.currentPage = currentPage;
        this.chatId = chatButtonClickEvent.getChatId();
        this.msgId = chatButtonClickEvent.getMsgId();
        this.userId = chatButtonClickEvent.getUserId();
        this.queryId = chatButtonClickEvent.getQueryId();
    }
}
