package ru.mail.jira.plugins.mrimsender.protocol.commands;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.core.Local;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;
import ru.mail.jira.plugins.mrimsender.protocol.MessageFormatter;

import java.util.Locale;

@Slf4j
public class CreateCommentCommand implements Command {
    private final IcqApiClient icqApiClient;
    private final MessageFormatter messageFormatter;
    private final UserData userData;
    private final LocaleManager localeManager;
    private final I18nResolver i18nResolver;

    public CreateCommentCommand(IcqApiClient icqApiClient, MessageFormatter messageFormatter, UserData userData, LocaleManager localeManager, I18nResolver i18nResolver) {
        this.icqApiClient = icqApiClient;
        this.messageFormatter = messageFormatter;
        this.userData = userData;
        this.localeManager = localeManager;
        this.i18nResolver = i18nResolver;
    }

    @Override
    public void execute(Event event) throws Exception {
        if (event instanceof CallbackQueryEvent) {
            log.debug("CreateCommentCommand execution started...");
            CallbackQueryEvent callbackQueryEvent = (CallbackQueryEvent) event;
            String queryId = callbackQueryEvent.getQueryId();
            String chatId = callbackQueryEvent.getMessage().getChat().getChatId();
            String issueKey = StringUtils.substringAfter(callbackQueryEvent.getCallbackData(), "-");
            String mrimLogin = callbackQueryEvent.getFrom().getUserId();
            ApplicationUser commentedUser = userData.getUserByMrimLogin(mrimLogin);
            Locale locale = localeManager.getLocaleFor(commentedUser);
            String message = i18nResolver.getText(locale, "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.insertComment.message", issueKey);
            icqApiClient.answerCallbackQuery(queryId, null, false, null);
            icqApiClient.sendMessageText(chatId, message, messageFormatter.getCancelButton(commentedUser));
            // TODO the string below doesn't work
            //chatsStateMap.put(chatId, issueKey);
            log.debug("JiraMessageQueueProcessor answerCommentButtonClick queue offer finished...");
        }

    }
}
