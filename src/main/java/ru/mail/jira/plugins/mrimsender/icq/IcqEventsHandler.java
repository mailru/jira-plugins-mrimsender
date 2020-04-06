package ru.mail.jira.plugins.mrimsender.icq;

import java.util.Map;
import java.util.logging.Handler;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;

public class IcqEventsHandler {
    Map<Consts.EventType, Handler> handlersMap;
    public IcqEventsHandler(Map<Consts.EventType, Handler> handlersMap) {
        this.handlersMap = handlersMap;
    }

    public void handleEvent(Event<?> icqEvent) {
        if (icqEvent.getType().equals(Consts.EventType.NEW_MESSAGE_TYPE.getTypeStrValue())) {

        }
        if (icqEvent.getType().equals(Consts.EventType.CALLBACK_QUERY_TYPE.getTypeStrValue())) {

        }

    }
}
