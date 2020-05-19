package ru.mail.jira.plugins.mrimsender.protocol.events.buttons;

import lombok.Getter;
import ru.mail.jira.plugins.mrimsender.protocol.events.Event;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.ButtonClickEvent;

/**
 * As a result of this event handled user should get next formatted message
 *
 *  ----------------------------------------------------
 *  1. KEY-123 Issue summary comes here ...
 *  2. KEY-124 Issue summary comes here ...
 *
 *  n. KEY-612 Issue summary comes here ...
 *
 *  | go next page button |  go prev page button  |
 *  ----------------------------------------------------
 */
@Getter
public class SearchIssuesClickEvent implements Event {
    // TODO когда будет поиск по jql, переделать на одно поле String jqlClause
    private final String jqlClause;
    private final String chatId;
    private final String userId;
    private final String queryId;

    public SearchIssuesClickEvent(ButtonClickEvent chatButtonClickEvent, String jqlClause) {
        this.chatId = chatButtonClickEvent.getChatId();
        this.userId = chatButtonClickEvent.getUserId();
        this.queryId = chatButtonClickEvent.getQueryId();
        this.jqlClause = jqlClause;
    }


}
