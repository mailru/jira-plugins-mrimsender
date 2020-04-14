package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.LocaleManager;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.MentionIssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.notification.JiraNotificationReason;
import com.atlassian.jira.notification.NotificationFilterContext;
import com.atlassian.jira.notification.NotificationFilterManager;
import com.atlassian.jira.notification.NotificationRecipient;
import com.atlassian.jira.notification.NotificationSchemeManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.scheme.SchemeEntity;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;
import ru.mail.jira.plugins.mrimsender.icq.dto.InlineKeyboardMarkupButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MrimsenderEventListener implements InitializingBean, DisposableBean {
    private static final Logger log = Logger.getLogger(MrimsenderEventListener.class);

    private final EventPublisher eventPublisher;
    private final GroupManager groupManager;
    private final NotificationFilterManager notificationFilterManager;
    private final NotificationSchemeManager notificationSchemeManager;
    private final PermissionManager permissionManager;
    private final ProjectRoleManager projectRoleManager;
    private final UserData userData;
    private final MessageFormatter messageFormatter;
    private final JiraMessageQueueProcessor jiraMessageQueueProcessor;
    private final I18nResolver i18nResolver;
    private final LocaleManager localeManager;

    public MrimsenderEventListener(EventPublisher eventPublisher,
                                   GroupManager groupManager,
                                   NotificationFilterManager notificationFilterManager,
                                   NotificationSchemeManager notificationSchemeManager,
                                   PermissionManager permissionManager,
                                   ProjectRoleManager projectRoleManager,
                                   UserData userData,
                                   MessageFormatter messageFormatter,
                                   JiraMessageQueueProcessor jiraMessageQueueProcessor,
                                   I18nResolver i18nResolver,
                                   LocaleManager localeManager) {
        this.eventPublisher = eventPublisher;
        this.groupManager = groupManager;
        this.notificationFilterManager = notificationFilterManager;
        this.notificationSchemeManager = notificationSchemeManager;
        this.permissionManager = permissionManager;
        this.projectRoleManager = projectRoleManager;
        this.userData = userData;
        this.messageFormatter = messageFormatter;
        this.jiraMessageQueueProcessor = jiraMessageQueueProcessor;
        this.i18nResolver = i18nResolver;
        this.localeManager = localeManager;
    }

    @Override
    public void afterPropertiesSet() {
        eventPublisher.register(this);
    }

    @Override
    public void destroy() {
        eventPublisher.unregister(this);
    }

    @SuppressWarnings("unused")
    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        try {
            if (issueEvent.isSendMail()) {
                Set<ApplicationUser> recipients = new HashSet<>();

                Set<NotificationRecipient> notificationRecipients = notificationSchemeManager.getRecipients(issueEvent);
                NotificationFilterContext context = notificationFilterManager.makeContextFrom(JiraNotificationReason.ISSUE_EVENT, issueEvent);
                for (SchemeEntity schemeEntity : notificationSchemeManager.getNotificationSchemeEntities(issueEvent.getProject(), issueEvent.getEventTypeId())) {
                    context = notificationFilterManager.makeContextFrom(context, com.atlassian.jira.notification.type.NotificationType.from(schemeEntity.getType()));
                    Set<NotificationRecipient> recipientsFromScheme = notificationSchemeManager.getRecipients(issueEvent, schemeEntity);
                    recipientsFromScheme = Sets.newHashSet(notificationFilterManager.recomputeRecipients(recipientsFromScheme, context));
                    notificationRecipients.addAll(recipientsFromScheme);
                }

                for (NotificationRecipient notificationRecipient : notificationRecipients) {
                    if (canSendEventToUser(notificationRecipient.getUser(), issueEvent))
                        recipients.add(notificationRecipient.getUser());
                }

                sendMessage(recipients, issueEvent, issueEvent.getIssue().getKey());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    @EventListener
    public void onMentionIssueEvent(MentionIssueEvent mentionIssueEvent) {
        try {
            List<ApplicationUser> recipients = new ArrayList<>();
            for (ApplicationUser user : mentionIssueEvent.getToUsers())
                if (!mentionIssueEvent.getCurrentRecipients().contains(new NotificationRecipient(user)))
                    recipients.add(user);
            sendMessage(recipients, mentionIssueEvent, mentionIssueEvent.getIssue().getKey());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private boolean canSendEventToUser(ApplicationUser user, IssueEvent issueEvent) {
        ProjectRole projectRole = null;
        String groupName = null;
        Issue issue = issueEvent.getIssue();
        if (issueEvent.getWorklog() != null) {
            projectRole = issueEvent.getWorklog().getRoleLevel();
            groupName = issueEvent.getWorklog().getGroupLevel();
        } else if (issueEvent.getComment() != null) {
            projectRole = issueEvent.getComment().getRoleLevel();
            groupName = issueEvent.getComment().getGroupLevel();
        }

        if (!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user))
            return false;
        if (groupName != null && !groupManager.isUserInGroup(user, groupName))
            return false;
        if (projectRole != null && !projectRoleManager.isUserInProjectRole(user, projectRole, issue.getProjectObject()))
            return false;

        return true;
    }

    private void sendMessage(Collection<ApplicationUser> recipients, Object event, String issueKey) {
        for (ApplicationUser recipient : recipients) {
            if (recipient.isActive() && userData.isEnabled(recipient)) {
                String mrimLogin = userData.getMrimLogin(recipient);
                if (StringUtils.isNotBlank(mrimLogin)) {
                    String message = null;
                    if (event instanceof IssueEvent)
                        message = messageFormatter.formatEvent(recipient, (IssueEvent) event);
                    if (event instanceof MentionIssueEvent)
                        message = messageFormatter.formatEvent((MentionIssueEvent) event);

                    if (message != null) {
                        /*List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
                        List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
                        InlineKeyboardMarkupButton button = new InlineKeyboardMarkupButton();
                        button.setText("Comment issue");
                        button.setCallbackData(String.join("-","comment", issueKey));
                        buttonsRow.add(button);
                        buttons.add(buttonsRow);
                        jiraMessageQueueProcessor.sendMessage(mrimLogin, message, buttons);*/
                        if (event instanceof IssueEvent) {
                            List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
                            List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
                            InlineKeyboardMarkupButton button = new InlineKeyboardMarkupButton();
                            button.setText(i18nResolver.getRawText(localeManager.getLocaleFor(recipient), "ru.mail.jira.plugins.mrimsender.mrimsenderEventListener.quickViewButton.text"));
                            button.setCallbackData(String.join("-","view", issueKey));
                            buttonsRow.add(button);
                            buttons.add(buttonsRow);
                            jiraMessageQueueProcessor.sendMessage(mrimLogin, message, buttons);
                        } else {
                            jiraMessageQueueProcessor.sendMessage(mrimLogin, message, null);
                        }
                    }
                }
            }
        }
    }
}
