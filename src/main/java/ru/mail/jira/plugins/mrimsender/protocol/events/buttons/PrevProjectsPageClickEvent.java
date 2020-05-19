package ru.mail.jira.plugins.mrimsender.protocol.events.buttons;

public class PrevProjectsPageClickEvent extends PrevPageClickEvent {
    public PrevProjectsPageClickEvent(ButtonClickEvent chatButtonClickEvent, int currentPage) {
        super(chatButtonClickEvent, currentPage);
    }
}
