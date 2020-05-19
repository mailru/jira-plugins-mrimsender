package ru.mail.jira.plugins.mrimsender.protocol.events.buttons;

import lombok.Getter;
import ru.mail.jira.plugins.mrimsender.protocol.events.buttons.ButtonClickEvent;

@Getter
public class SearchByJqlClickEvent {
    private final String chatId;
    private final String userId;
    private final String queryId;

    public SearchByJqlClickEvent(ButtonClickEvent chatButtonClickEvent) {
        this.queryId = chatButtonClickEvent.getQueryId();
        this.userId = chatButtonClickEvent.getUserId();
        this.chatId = chatButtonClickEvent.getChatId();
    }

}
