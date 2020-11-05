package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;

@Getter
public class NextIssueCommentsPageClickEvent extends NextPageClickEvent {
    private final String issueKey;

    public NextIssueCommentsPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage, String issueKey) {
        super(chatButtonClickEvent, currentPage);
        this.issueKey = issueKey;
    }
}
