package ru.mail.jira.plugins.mrimsender.protocol.commands;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;
import ru.mail.jira.plugins.mrimsender.protocol.MessageFormatter;

@Slf4j
public class ViewIssueCommand implements Command {
    private final UserData userData;
    private final IssueManager issueManager;
    private final PermissionManager permissionManager;
    private final IcqApiClient icqApiClient;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final MessageFormatter messageFormatter;

    public ViewIssueCommand(UserData userData, IssueManager issueManager, PermissionManager permissionManager, IcqApiClient icqApiClient, I18nResolver i18nResolver, LocaleManager localeManager, MessageFormatter messageFormatter) {
        this.userData = userData;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.icqApiClient = icqApiClient;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
        this.messageFormatter = messageFormatter;
    }

    @Override
    public void execute(Event event) throws Exception {
        if (event instanceof CallbackQueryEvent) {
            log.debug("ViewIssueCommand execution started...");
            CallbackQueryEvent callbackQueryEvent = (CallbackQueryEvent)event;
            String issueKey = StringUtils.substringAfter(callbackQueryEvent.getCallbackData(), "-");
            String chatId = callbackQueryEvent.getMessage().getChat().getChatId();
            String queryId = callbackQueryEvent.getQueryId();
            String mrimLogin = callbackQueryEvent.getFrom().getUserId();
            icqApiClient.answerCallbackQuery(queryId, null, false, null);
            Issue currentIssue = issueManager.getIssueByCurrentKey(issueKey);
            ApplicationUser currentUser = userData.getUserByMrimLogin(mrimLogin);
            if (currentUser != null && currentIssue != null) {
                if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, currentIssue, currentUser)) {
                    icqApiClient.sendMessageText(chatId, messageFormatter.createIssueSummary(currentIssue, currentUser), messageFormatter.getIssueButtons(issueKey, currentUser));
                    log.debug("ViewIssueCommand message sent...");
                } else {
                    icqApiClient.sendMessageText(chatId, i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"), null);
                    log.debug("ViewIssueCommand no permissions message sent...");
                }
            }
            log.debug("ViewIssueCommand execution finished...");
        }
    }
}
