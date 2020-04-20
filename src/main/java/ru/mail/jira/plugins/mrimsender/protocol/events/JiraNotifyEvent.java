package ru.mail.jira.plugins.mrimsender.protocol.events;

import lombok.Getter;
import lombok.Setter;
import ru.mail.jira.plugins.mrimsender.icq.dto.InlineKeyboardMarkupButton;

import java.util.List;

@Getter
@Setter
public class JiraNotifyEvent {
    private final String chatId;
    private final String message;
    private final List<List<InlineKeyboardMarkupButton>> buttons;

    public JiraNotifyEvent(String chatId, String message, List<List<InlineKeyboardMarkupButton>> buttons) {
        this.chatId = chatId;
        this.message = message;
        this.buttons = buttons;
    }
}
