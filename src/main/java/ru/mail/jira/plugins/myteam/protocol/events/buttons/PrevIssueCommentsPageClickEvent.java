package ru.mail.jira.plugins.myteam.protocol.events.buttons;

import lombok.Getter;

@Getter
public class PrevIssueCommentsPageClickEvent extends PrevPageClickEvent {
    private final String issueKey;

    public PrevIssueCommentsPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage, String issueKey) {
        super(chatButtonClickEvent, currentPage);
        this.issueKey = issueKey;
    }
}
