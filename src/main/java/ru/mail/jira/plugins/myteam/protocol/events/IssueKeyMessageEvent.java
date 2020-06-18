package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;

@Getter
public class IssueKeyMessageEvent {
    private final String chatId;
    private final String issueKey;
    private final String userId;

    public IssueKeyMessageEvent(ChatMessageEvent chatMessageEvent) {
        this.chatId = chatMessageEvent.getChatId();
        this.issueKey = chatMessageEvent.getMessage().trim().toUpperCase();
        this.userId = chatMessageEvent.getUerId();
    }
}
