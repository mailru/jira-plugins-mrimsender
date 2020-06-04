package ru.mail.jira.plugins.mrimsender.protocol.listeners;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.Subscribe;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.ChatType;
import ru.mail.jira.plugins.mrimsender.protocol.MessageFormatter;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowDefaultMessageEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowHelpEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowIssueEvent;
import ru.mail.jira.plugins.mrimsender.protocol.events.ShowMenuEvent;

import java.io.IOException;
import java.util.Locale;

@Slf4j
public class ChatCommandListener {
    private final IcqApiClient icqApiClient;
    private final UserData userData;
    private final MessageFormatter messageFormatter;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final IssueManager issueManager;
    private final PermissionManager permissionManager;

    public ChatCommandListener(IcqApiClient icqApiClient,
                               UserData userData,
                               MessageFormatter messageFormatter,
                               I18nResolver i18nResolver,
                               LocaleManager localeManager,
                               IssueManager issueManager,
                               PermissionManager permissionManager) {
        this.icqApiClient = icqApiClient;
        this.userData = userData;
        this.messageFormatter = messageFormatter;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
    }

    @Subscribe
    public void onShowHelpEvent(ShowHelpEvent showHelpEvent) throws IOException, UnirestException {
        log.debug("ShowHelpEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showHelpEvent.getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            if (showHelpEvent.getChatType() == ChatType.GROUP)
                icqApiClient.sendMessageText(showHelpEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.groupChat.helpMessage.text"));
            else
                icqApiClient.sendMessageText(showHelpEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.helpMessage.text"));
        }
        log.debug("ShowHelpEvent handling finished");
    }

    @Subscribe
    public void onShowMenuEvent(ShowMenuEvent showMenuEvent) throws IOException, UnirestException {
        log.debug("ShowDefaultMenuEvent handling started...");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showMenuEvent.getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            icqApiClient.sendMessageText(showMenuEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.mainMenu.text"), messageFormatter.getMenuButtons(currentUser));
        }
        log.debug("JiraMessageQueueProcessor showDefaultMenu finished...");
    }

    @Subscribe
    public void onShowIssueEvent(ShowIssueEvent showIssueEvent) throws IOException, UnirestException {
        log.debug("ShowIssueEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showIssueEvent.getUserId());
        Issue issueToShow = issueManager.getIssueByCurrentKey(showIssueEvent.getIssueKey());
        if (currentUser != null && issueToShow != null) {
            if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issueToShow, currentUser)) {
                icqApiClient.sendMessageText(showIssueEvent.getChatId(), messageFormatter.createIssueSummary(issueToShow, currentUser), messageFormatter.getIssueButtons(issueToShow.getKey(), currentUser));
            } else {
                icqApiClient.sendMessageText(showIssueEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.quickViewButton.noPermissions"));
            }
        } else if (currentUser != null) {
            icqApiClient.sendMessageText(showIssueEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.mrimsender.icqEventsListener.newIssueKeyMessage.error.issueNotFound"));
        }
        log.debug("ShowIssueEvent handling finished");
    }


    @Subscribe
    public void onShowDefaultMessageEvent(ShowDefaultMessageEvent showDefaultMessageEvent) throws IOException, UnirestException {
        log.debug("ShowDefaultMessageEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showDefaultMessageEvent.getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            icqApiClient.sendMessageText(showDefaultMessageEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.mrimsender.icqEventsListener.defaultMessage.text"), messageFormatter.getMenuButtons(currentUser));
        }
        log.debug("ShowDefaultMessageEvent handling finished");
    }
}
