package ru.mail.jira.plugins.mrimsender.protocol;

import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@FunctionalInterface
public interface BotChatCommand {
    void execute(NewMessageEvent newMessageEvent);
}
