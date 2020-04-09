package ru.mail.jira.plugins.mrimsender.icq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.JiraMessageHandler;

public class IcqEventsHandlerImpl implements IcqEventsHandler {
    private final JiraMessageHandler jiraMessageHandler;
    private static final Logger log = LoggerFactory.getLogger(IcqEventsHandlerImpl.class);

    public IcqEventsHandlerImpl(JiraMessageHandler jiraMessageHandler) {
        this.jiraMessageHandler = jiraMessageHandler;
    }

    @Override
    public void handleEvent(NewMessageEvent newMessageEvent) {
        log.debug("IcqEventsHandlerImpl handling NewMessageEvent started");
        jiraMessageHandler.sendMessage(newMessageEvent.getChat().getChatId(), "Bot successfully started");
        log.debug("IcqEventsHandlerImpl handling NewMessageEvent finished");
    }

    @Override
    public void handleEvent(CallbackQueryEvent callbackQueryEvent) {
        log.debug("IcqEventsHandlerImpl handling CallbackQueryEvent started");
        jiraMessageHandler.answerButtonClick(callbackQueryEvent.getQueryId(), "button clicked");
        log.debug("IcqEventsHandlerImpl handling NewMessageEvent finished");

    }
}
