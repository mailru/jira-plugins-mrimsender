package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;

@Getter
public class ShowIssueEvent {
    private final String chatId;
    private final String issueKey;
    private final String userId;

    public ShowIssueEvent(ChatMessageEvent chatMessageEvent) {
        this.chatId = chatMessageEvent.getChatId();
        this.userId = chatMessageEvent.getUerId();
        this.issueKey = StringUtils.substringAfter(chatMessageEvent.getMessage().trim().toLowerCase(),"/issue")
                                   .trim()
                                   .toUpperCase();
    }
}
