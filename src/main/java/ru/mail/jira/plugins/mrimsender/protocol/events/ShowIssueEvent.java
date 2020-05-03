package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
public class ShowIssueEvent {
    private final String chatId;
    private final String issueKey;
    private final String userId;

    public ShowIssueEvent(ChatMessageEvent chatMessageEvent) {
        this.chatId = chatMessageEvent.getChatId();
        this.userId = chatMessageEvent.getUerId();
        this.issueKey = StringUtils.substringAfter(chatMessageEvent.getMessage().trim(),"/issue")
                                   .trim()
                                   .toUpperCase();
    }
}
