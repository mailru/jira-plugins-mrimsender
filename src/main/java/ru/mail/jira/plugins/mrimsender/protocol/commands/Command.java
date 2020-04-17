package ru.mail.jira.plugins.mrimsender.protocol.commands;

import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;

@FunctionalInterface
public interface Command {
    public void execute(Event event) throws Exception;
}
