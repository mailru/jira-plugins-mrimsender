package ru.mail.jira.plugins.myteam.protocol.events;

import lombok.Getter;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class ShowDefaultMessageEvent {
    private final String chatId;
    private final String userId;
    private final String message;
    private final boolean hasForwards;
    private final List<Forward> forwardList;

    public ShowDefaultMessageEvent(ChatMessageEvent chatMessageEvent) {
        chatId = chatMessageEvent.getChatId();
        userId = chatMessageEvent.getUerId();
        message = chatMessageEvent.getMessage();
        hasForwards = chatMessageEvent.isHasForwards();
        forwardList = (hasForwards) ? chatMessageEvent.getMessageParts()
                                                      .stream()
                                                      .filter(part -> part instanceof Forward)
                                                      .map(part -> (Forward)part)
                                                      .collect(Collectors.toList()) : null;

    }
}
