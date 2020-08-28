package ru.mail.jira.plugins.myteam.protocol.listeners;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.eventbus.Subscribe;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.myteam.Utils;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;
import ru.mail.jira.plugins.myteam.myteam.dto.ChatType;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.protocol.events.ShowDefaultMessageEvent;
import ru.mail.jira.plugins.myteam.protocol.events.ShowHelpEvent;
import ru.mail.jira.plugins.myteam.protocol.events.ShowIssueEvent;
import ru.mail.jira.plugins.myteam.protocol.events.ShowMenuEvent;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;

@Slf4j
public class ChatCommandListener {
    private final MyteamApiClient myteamApiClient;
    private final UserData userData;
    private final MessageFormatter messageFormatter;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;
    private final IssueManager issueManager;
    private final PermissionManager permissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final String JIRA_BASE_URL;

    public ChatCommandListener(MyteamApiClient myteamApiClient,
                               UserData userData,
                               MessageFormatter messageFormatter,
                               I18nResolver i18nResolver,
                               LocaleManager localeManager,
                               IssueManager issueManager,
                               PermissionManager permissionManager,
                               JiraAuthenticationContext jiraAuthenticationContext,
                               ApplicationProperties applicationProperties) {
        this.myteamApiClient = myteamApiClient;
        this.userData = userData;
        this.messageFormatter = messageFormatter;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.JIRA_BASE_URL = applicationProperties.getString(APKeys.JIRA_BASEURL);
    }

    @Subscribe
    public void onShowHelpEvent(ShowHelpEvent showHelpEvent) throws IOException, UnirestException {
        log.debug("ShowHelpEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showHelpEvent.getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            if (showHelpEvent.getChatType() == ChatType.GROUP)
                myteamApiClient.sendMessageText(showHelpEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.groupChat.helpMessage.text"));
            else
                myteamApiClient.sendMessageText(showHelpEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.helpMessage.text"));
        }
        log.debug("ShowHelpEvent handling finished");
    }

    @Subscribe
    public void onShowMenuEvent(ShowMenuEvent showMenuEvent) throws IOException, UnirestException {
        log.debug("ShowDefaultMenuEvent handling started...");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showMenuEvent.getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            myteamApiClient.sendMessageText(showMenuEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.myteam.messageQueueProcessor.mainMenu.text"), messageFormatter.getMenuButtons(currentUser));
        }
        log.debug("JiraMessageQueueProcessor showDefaultMenu finished...");
    }

    @Subscribe
    public void onShowIssueEvent(ShowIssueEvent showIssueEvent) throws IOException, UnirestException {
        log.debug("ShowIssueEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showIssueEvent.getUserId());
        ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
        try {
            jiraAuthenticationContext.setLoggedInUser(currentUser);
            Issue issueToShow = issueManager.getIssueByKeyIgnoreCase(showIssueEvent.getIssueKey());
            if (currentUser != null && issueToShow != null) {
                if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issueToShow, currentUser)) {
                    myteamApiClient.sendMessageText(showIssueEvent.getChatId(), messageFormatter.createIssueSummary(issueToShow, currentUser), messageFormatter.getIssueButtons(issueToShow.getKey(), currentUser));
                } else {
                    myteamApiClient.sendMessageText(showIssueEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
                }
            } else if (currentUser != null) {
                myteamApiClient.sendMessageText(showIssueEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.issueNotFound"));
            }
            log.debug("ShowIssueEvent handling finished");
        } finally {
            jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
        }
    }


    @Subscribe
    public void onShowDefaultMessageEvent(ShowDefaultMessageEvent showDefaultMessageEvent) throws IOException, UnirestException {
        log.debug("ShowDefaultMessageEvent handling started");
        ApplicationUser currentUser = userData.getUserByMrimLogin(showDefaultMessageEvent.getUserId());
        if (currentUser != null) {
            Locale locale = localeManager.getLocaleFor(currentUser);
            if (showDefaultMessageEvent.isHasForwards()) {
                Forward forward = showDefaultMessageEvent.getForwardList().get(0);
                String forwardMessageText = forward.getMessage().getText();
                URL issueUrl = Utils.tryFindUrlByPrefixInStr(forwardMessageText, JIRA_BASE_URL);
                if (issueUrl != null) {
                    String issueKey = StringUtils.substringAfterLast(issueUrl.getPath(), "/");
                    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
                    try {
                        jiraAuthenticationContext.setLoggedInUser(currentUser);
                        Issue issueToShow = issueManager.getIssueByKeyIgnoreCase(issueKey);
                        if (issueToShow != null) {
                            if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issueToShow, currentUser)) {
                                myteamApiClient.sendMessageText(showDefaultMessageEvent.getChatId(), messageFormatter.createIssueSummary(issueToShow, currentUser), messageFormatter.getIssueButtons(issueToShow.getKey(), currentUser));
                            } else {
                                myteamApiClient.sendMessageText(showDefaultMessageEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
                            }
                        } else {
                            myteamApiClient.sendMessageText(showDefaultMessageEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.issueNotFound"));
                        }
                        log.debug("ShowDefaultMessageEvent handling finished");
                    } finally {
                        jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
                    }
                    return;
                }
            }
            URL issueUrl = Utils.tryFindUrlByPrefixInStr(showDefaultMessageEvent.getMessage(), JIRA_BASE_URL);
            if (issueUrl != null) {
                String issueKey = StringUtils.substringAfterLast(issueUrl.getPath(), "/");
                ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
                try {
                    jiraAuthenticationContext.setLoggedInUser(currentUser);
                    Issue issueToShow = issueManager.getIssueByKeyIgnoreCase(issueKey);
                    if (issueToShow != null) {
                        if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issueToShow, currentUser)) {
                            myteamApiClient.sendMessageText(showDefaultMessageEvent.getChatId(), messageFormatter.createIssueSummary(issueToShow, currentUser), messageFormatter.getIssueButtons(issueToShow.getKey(), currentUser));
                        } else {
                            myteamApiClient.sendMessageText(showDefaultMessageEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.myteam.messageQueueProcessor.quickViewButton.noPermissions"));
                        }
                    } else {
                        myteamApiClient.sendMessageText(showDefaultMessageEvent.getChatId(), i18nResolver.getRawText(localeManager.getLocaleFor(currentUser), "ru.mail.jira.plugins.myteam.myteamEventsListener.newIssueKeyMessage.error.issueNotFound"));
                    }
                    log.debug("ShowDefaultMessageEvent handling finished");
                } finally {
                    jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
                }
                return;
            }
            myteamApiClient.sendMessageText(showDefaultMessageEvent.getChatId(), i18nResolver.getRawText(locale, "ru.mail.jira.plugins.myteam.myteamEventsListener.defaultMessage.text"), messageFormatter.getMenuButtons(currentUser));
        }
        log.debug("ShowDefaultMessageEvent handling finished");
    }
}
