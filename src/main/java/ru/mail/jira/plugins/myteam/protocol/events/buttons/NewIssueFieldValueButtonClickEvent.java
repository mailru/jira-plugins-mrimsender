package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.protocol.IssueCreationDto;
import ru.mail.jira.plugins.myteam.protocol.events.NewIssueValueEvent;

@Getter
public class NewIssueFieldValueButtonClickEvent implements NewIssueValueEvent {
    private final String queryId;
    private final String userId;
    private final String chatId;
    private final String fieldValue;
    private final IssueCreationDto issueCreationDto;
    private final Integer currentFieldNum;

    public NewIssueFieldValueButtonClickEvent(ButtonClickEvent buttonClickEvent, IssueCreationDto issueCreationDto, Integer currentFieldNum) {
        this.queryId = buttonClickEvent.getQueryId();
        this.chatId = buttonClickEvent.getChatId();
        this.userId = buttonClickEvent.getUserId();
        this.fieldValue = StringUtils.substringAfter(buttonClickEvent.getCallbackData(), "-");
        this.issueCreationDto = issueCreationDto;
        this.currentFieldNum = currentFieldNum;
    }
}
