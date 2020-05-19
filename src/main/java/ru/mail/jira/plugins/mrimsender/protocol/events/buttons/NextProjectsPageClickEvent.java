package ru.mail.jira.plugins.mrimsender.protocol.events.buttons;

public class NextProjectsPageClickEvent extends NextPageClickEvent {
    public NextProjectsPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage) {
        super(chatButtonClickEvent, currentPage);
    }
}
