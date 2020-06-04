package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import ru.mail.jira.plugins.mrimsender.protocol.IssueCreationDto;

@Getter
public class SelectedIssueTypeMessageEvent {
    private final String userId;
    private final String chatId;
    private final IssueCreationDto issueCreationDto;
    private final String selectedIssueTypePosition;

    public SelectedIssueTypeMessageEvent(ChatMessageEvent chatMessageEvent, IssueCreationDto issueCreationDto) {
        userId = chatMessageEvent.getUerId();
        chatId = chatMessageEvent.getChatId();
        this.issueCreationDto = issueCreationDto;
        selectedIssueTypePosition = chatMessageEvent.getMessage().trim().toUpperCase();
    }
}
