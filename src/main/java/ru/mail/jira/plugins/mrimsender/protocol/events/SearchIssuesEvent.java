package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;


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
 *
 *  Instead of SearchIssuesClickEvent class this event published when JqlClause came from user message
 */
@Getter
public class SearchIssuesEvent {
    private final String chatId;
    private final String jqlClause;
    private final String userId;

    public SearchIssuesEvent(ChatMessageEvent chatMessageEvent) {
        this.chatId = chatMessageEvent.getChatId();
        this.jqlClause = chatMessageEvent.getMessage().trim();
        this.userId = chatMessageEvent.getUerId();
    }
}
