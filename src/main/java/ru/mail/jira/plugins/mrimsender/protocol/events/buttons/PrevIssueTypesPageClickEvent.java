package ru.mail.jira.plugins.mrimsender.protocol.events.buttons;

import lombok.Getter;

@Getter
public class PrevIssueTypesPageClickEvent extends PrevPageClickEvent {
    private final String selectedProjectKey;

    public PrevIssueTypesPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage, String selectedProjectKey) {
        super(chatButtonClickEvent, currentPage);
        this.selectedProjectKey = selectedProjectKey;
    }
}
