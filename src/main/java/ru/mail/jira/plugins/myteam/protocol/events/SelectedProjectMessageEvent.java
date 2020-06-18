package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;

@Getter
public class SelectedProjectMessageEvent {
    private final String userId;
    private final String chatId;
    private final String selectedProjectKey;
    private final IssueCreationDto issueCreationDto;

    public SelectedProjectMessageEvent(ChatMessageEvent chatMessageEvent, IssueCreationDto issueCreationDto) {
        userId = chatMessageEvent.getUerId();
        chatId = chatMessageEvent.getChatId();
        selectedProjectKey = chatMessageEvent.getMessage().trim().toUpperCase();
        this.issueCreationDto = issueCreationDto;
    }
}
