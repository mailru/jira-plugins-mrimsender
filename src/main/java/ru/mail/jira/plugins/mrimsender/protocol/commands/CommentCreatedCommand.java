package ru.mail.jira.plugins.mrimsender.protocol.commands;

import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.IcqApiClient;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

@Slf4j
public class CommentCreatedCommand implements Command {

    private final UserData userData;
    private final CommentManager commentManager;
    private final IssueManager issueManager;
    private final PermissionManager permissionManager;
    private final IcqApiClient icqApiClient;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;

    public CommentCreatedCommand(UserData userData, CommentManager commentManager, IssueManager issueManager, PermissionManager permissionManager, IcqApiClient icqApiClient, I18nResolver i18nResolver, LocaleManager localeManager) {
        this.userData = userData;
        this.commentManager = commentManager;
        this.issueManager = issueManager;
        this.permissionManager = permissionManager;
        this.icqApiClient = icqApiClient;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
    }

    @Override
    public void execute(Event event) throws Exception {
        if (event instanceof NewMessageEvent) {
            NewMessageEvent newMessageEvent = (NewMessageEvent)event;
            log.debug("CreateCommentCommand execution started...");
            String chatId = newMessageEvent.getChat().getChatId();
            String issueKey = "";
            // TODO below string still isn't working !!!
            //String issueKey = chatsStateMap.remove(chatId);
            String mrimLogin = newMessageEvent.getFrom().getUserId();
            String commentMessage = newMessageEvent.getText();
            ApplicationUser commentedUser = userData.getUserByMrimLogin(mrimLogin);
            Issue commentedIssue = issueManager.getIssueByCurrentKey(issueKey);
            if (commentedUser != null && commentedIssue != null) {
                if (permissionManager.hasPermission(ProjectPermissions.ADD_COMMENTS, commentedIssue, commentedUser)) {
                    commentManager.create(commentedIssue, commentedUser, commentMessage, true);
                    log.debug("CreateCommentCommand comment created...");
                    icqApiClient.sendMessageText(chatId, i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.commentCreated"), null);
                    log.debug("CreateCommentCommand new comment created message sent...");
                } else {
                    log.debug("CreateCommentCommand permissions violation occurred...");
                    icqApiClient.sendMessageText(chatId,i18nResolver.getText(localeManager.getLocaleFor(commentedUser), "ru.mail.jira.plugins.mrimsender.messageQueueProcessor.commentButton.noPermissions"), null);
                    log.debug("CreateCommentCommand not enough permissions message sent...");
                }
            }
            log.debug("CreateCommentCommand execution finished...");
        }
    }

}
