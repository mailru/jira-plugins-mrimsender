package ru.mail.jira.plugins.mrimsender.protocol.events.buttons;

import lombok.Getter;

@Getter
public class NextIssueTypesPageClickEvent extends NextPageClickEvent {
    private final String selectedProjectKey;

    public NextIssueTypesPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage, String selectedProjectKey) {
        super(chatButtonClickEvent, currentPage);
        this.selectedProjectKey = selectedProjectKey;
    }
}