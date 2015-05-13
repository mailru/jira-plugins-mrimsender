package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.MentionIssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.notification.NotificationRecipient;
import com.atlassian.jira.notification.NotificationSchemeManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.configuration.UserData;

import java.util.*;

public class MrimsenderEventListener implements InitializingBean, DisposableBean {
    private static final Logger log = Logger.getLogger(MrimsenderEventListener.class);

    private final EventPublisher eventPublisher;
    private final GroupManager groupManager;
    private final NotificationSchemeManager notificationSchemeManager;
    private final PermissionManager permissionManager;
    private final PluginData pluginData;
    private final ProjectRoleManager projectRoleManager;
    private final UserData userData = new UserData();

    public MrimsenderEventListener(EventPublisher eventPublisher, GroupManager groupManager, NotificationSchemeManager notificationSchemeManager, PermissionManager permissionManager, PluginData pluginData, ProjectRoleManager projectRoleManager) {
        this.eventPublisher = eventPublisher;
        this.groupManager = groupManager;
        this.notificationSchemeManager = notificationSchemeManager;
        this.permissionManager = permissionManager;
        this.pluginData = pluginData;
        this.projectRoleManager = projectRoleManager;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        MrimsenderThread.startInstance();
        eventPublisher.register(this);
    }
 
    @Override
    public void destroy() throws Exception {
        MrimsenderThread.stopInstance();
        eventPublisher.unregister(this);
    }

    private void sendMessage(Collection<ApplicationUser> recipients, Object event) {
        if (!StringUtils.isEmpty(pluginData.getLogin()) && !StringUtils.isEmpty(pluginData.getPassword()))
            for (ApplicationUser recipient : recipients) {
                String mrimLogin = userData.getMrimLogin(recipient);
                if (recipient.isActive() && !StringUtils.isBlank(mrimLogin) && userData.isEnabled(recipient)) {
                    String message = null;
                    if (event instanceof IssueEvent)
                        message = new MessageFormatter(recipient).formatEvent((IssueEvent) event);
                    if (event instanceof MentionIssueEvent)
                        message = new MessageFormatter(recipient).formatEvent((MentionIssueEvent) event);
                    if (message != null)
                        MrimsenderThread.sendMessage(mrimLogin, message);
                }
            }
    }

    @SuppressWarnings("unused")
    @EventListener
    public void onIssueEvent(IssueEvent issueEvent) {
        try {
            if (issueEvent.isSendMail()) {
                Set<ApplicationUser> recipients = new HashSet<ApplicationUser>();

                for (NotificationRecipient notificationRecipient : notificationSchemeManager.getRecipients(issueEvent))
                    recipients.add(notificationRecipient.getUser());

                if (issueEvent.getWorklog() != null)
                    recipients = getFilteredRecipients(recipients, issueEvent.getWorklog().getRoleLevel(), issueEvent.getWorklog().getGroupLevel(), issueEvent.getIssue());
                else if (issueEvent.getComment() != null)
                    recipients = getFilteredRecipients(recipients, issueEvent.getComment().getRoleLevel(), issueEvent.getComment().getGroupLevel(), issueEvent.getIssue());

                sendMessage(recipients, issueEvent);
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    private Set<ApplicationUser> getFilteredRecipients(Collection<ApplicationUser> recipients, ProjectRole projectRole, String groupName, Issue issue) {
        Set<ApplicationUser> result = new HashSet<ApplicationUser>();
        for (ApplicationUser user : recipients) {
            if (!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user))
                 continue;
            if (groupName != null && !groupManager.isUserInGroup(user.getName(), groupName))
                continue;
            if (projectRole != null && !projectRoleManager.isUserInProjectRole(user, projectRole, issue.getProjectObject()))
                continue;
            result.add(user);
        }
        return result;
    }

    @SuppressWarnings("unused")
    @EventListener
    public void onMentionIssueEvent(MentionIssueEvent mentionIssueEvent) {
        try {
            List<ApplicationUser> recipients = new ArrayList<ApplicationUser>();
            for (User directoryUser : mentionIssueEvent.getToUsers()) {
                ApplicationUser user = ApplicationUsers.from(directoryUser);
                if (!mentionIssueEvent.getCurrentRecipients().contains(new NotificationRecipient(user)))
                    recipients.add(user);
            }
            sendMessage(recipients, mentionIssueEvent);
        } catch (Exception e) {
            log.error(e);
        }
    }
}
