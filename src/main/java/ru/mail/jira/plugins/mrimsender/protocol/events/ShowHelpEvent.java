package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.ChatType;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Getter
public class ShowHelpEvent implements Event {
    private final String chatId;
    private final String userId;
    private final ChatType chatType;

    public ShowHelpEvent(ChatMessageEvent chatMessageEvent) {
        this.chatId = chatMessageEvent.getChatId();
        this.userId = chatMessageEvent.getUerId();
        this.chatType = chatMessageEvent.getChatType();
    }
}
