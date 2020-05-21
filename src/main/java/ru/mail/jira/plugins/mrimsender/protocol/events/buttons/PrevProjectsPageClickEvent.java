package ru.mail.jira.plugins.mrimsender.protocol.events.buttons;

import lombok.Getter;
import ru.mail.jira.plugins.mrimsender.protocol.IssueCreationDto;

@Getter
public class PrevProjectsPageClickEvent extends PrevPageClickEvent {
    private final IssueCreationDto issueCreationDto;

    public PrevProjectsPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage, IssueCreationDto issueCreationDto) {
        super(chatButtonClickEvent, currentPage);
        this.issueCreationDto = issueCreationDto;
    }
}
