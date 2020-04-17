package ru.mail.jira.plugins.mrimsender.protocol.commands;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.sal.api.message.I18nResolver;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;

@Slf4j
public class CancelCommand implements Command {
    private final IcqApiClient icqApiClient;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final UserData userData;

    public CancelCommand(IcqApiClient icqApiClient, I18nResolver i18nResolver, LocaleManager localeManager, UserData userData) {
        this.icqApiClient = icqApiClient;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
        this.userData = userData;
    }

    @Override
    public void execute(Event event) throws Exception {
        if (event instanceof CallbackQueryEvent) {
            log.debug("CancelCommand execution started...");
            CallbackQueryEvent callbackQueryEvent = (CallbackQueryEvent)event;
            String queryId = callbackQueryEvent.getQueryId();
            String userId = callbackQueryEvent.getFrom().getUserId();
            String message = i18nResolver.getRawText(localeManager.getLocaleFor(userData.getUserByMrimLogin(userId)), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.cancelComment.message");
            icqApiClient.answerCallbackQuery(queryId, message, false, null);
            // TODO below string isn't working
            //chatsStateMap.remove(chatId);
            log.debug("CancelCommand execution finished...");
        }
    }
}
