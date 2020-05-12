package ru.mail.jira.plugins.mrimsender.protocol.events;

import com.atlassian.query.Query;
import lombok.Getter;


/**
 * As a result of this event handled user message on which "prev" button was clicked should be edited
 * and previous page of content should shown to user
 */
@Getter
public class PrevPageClickEvent implements Event {
    private final String chatId;
    private final long msgId;
    private final String userId;
    private final int currentPage;
    private final Query currentJqlQueryClause;
    private final String queryId;

    public PrevPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage, Query currentJqlQueryClause) {
        this.currentJqlQueryClause = currentJqlQueryClause;
        this.currentPage = currentPage;
        this.chatId = chatButtonClickEvent.getChatId();
        this.msgId = chatButtonClickEvent.getMsgId();
        this.userId = chatButtonClickEvent.getUserId();
        this.queryId = chatButtonClickEvent.getQueryId();
    }
}

