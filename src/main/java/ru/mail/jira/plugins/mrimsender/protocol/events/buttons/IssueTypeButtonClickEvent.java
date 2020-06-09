package ru.mail.jira.plugins.mrimsender.protocol.events.buttons;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.protocol.IssueCreationDto;

@Getter
public class IssueTypeButtonClickEvent {
    private final String queryId;
    private final String chatId;
    private final String userId;
    private final IssueCreationDto issueCreationDto;
    private final String selectedIssueTypeId;

    public IssueTypeButtonClickEvent(ButtonClickEvent buttonClickEvent,IssueCreationDto issueCreationDto) {
        this.queryId = buttonClickEvent.getQueryId();
        this.chatId = buttonClickEvent.getChatId();
        this.userId = buttonClickEvent.getUserId();
        this.selectedIssueTypeId =  StringUtils.substringAfter(buttonClickEvent.getCallbackData(), "-");
        this.issueCreationDto = issueCreationDto;
    }
}
