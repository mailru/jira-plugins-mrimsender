package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;

/**
 * statefull ViewIssueClickEvent -> waits for issue key from user input
 */
@Getter
public class ShowIssueClickEvent {
    private final String queryId;
    private final String chatId;
    private final String userId;

    public ShowIssueClickEvent(ButtonClickEvent chatButtonClickEvent) {
        this.queryId = chatButtonClickEvent.getQueryId();
        this.chatId = chatButtonClickEvent.getChatId();
        this.userId = chatButtonClickEvent.getUserId();
    }
}
