package ru.mail.jira.plugins.mrimsender.protocol;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.mrimsender.icq.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class IcqEventsPublisher {
    private static final String THREAD_NAME_PREFIX = "icq-events-dispatcher-thread-pool";

    private final ExecutorService executorService = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setNameFormat(THREAD_NAME_PREFIX).build());
    private final AsyncEventBus asyncEventBus = new AsyncEventBus(executorService, (exception, context) -> log.error(String.format("Event occurred in subscriber = %s", context.getSubscriber().toString()), exception));

    public IcqEventsPublisher( IcqEventsListener icqEventsListener) {
        asyncEventBus.register(icqEventsListener);
    }

    public void publishEvent(Event event) {
        asyncEventBus.post(event);
    }

    public void publishJiraNotifyEvent(JiraNotifyEvent jiraNotifyEvent) {
        asyncEventBus.post(jiraNotifyEvent);
    }

    @Setter
    @Getter
    public static class JiraNotifyEvent {
        private final String chatId;
        private final String message;
        private final List<List<InlineKeyboardMarkupButton>> buttons;

        public JiraNotifyEvent(String chatId, String message, List<List<InlineKeyboardMarkupButton>> buttons) {
            this.chatId = chatId;
            this.message = message;
            this.buttons = buttons;
        }
    }
}
