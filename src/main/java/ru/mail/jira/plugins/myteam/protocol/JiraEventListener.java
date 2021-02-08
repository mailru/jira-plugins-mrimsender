/* (C)2020 */
package ru.mail.jira.plugins.myteam.protocol;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
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
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.configuration.UserData;
import ru.mail.jira.plugins.myteam.protocol.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.protocol.listeners.MyteamEventsListener;

@Component
public class JiraEventListener implements InitializingBean, DisposableBean {
  private static final Logger log = Logger.getLogger(JiraEventListener.class);

  private final EventPublisher eventPublisher;
  private final GroupManager groupManager;
  private final NotificationFilterManager notificationFilterManager;
  private final NotificationSchemeManager notificationSchemeManager;
  private final PermissionManager permissionManager;
  private final ProjectRoleManager projectRoleManager;
  private final UserData userData;
  private final MessageFormatter messageFormatter;
  private final MyteamEventsListener myteamEventsListener;

  @Autowired
  public JiraEventListener(
      @ComponentImport EventPublisher eventPublisher,
      @ComponentImport GroupManager groupManager,
      @ComponentImport NotificationFilterManager notificationFilterManager,
      @ComponentImport NotificationSchemeManager notificationSchemeManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport ProjectRoleManager projectRoleManager,
      UserData userData,
      MessageFormatter messageFormatter,
      MyteamEventsListener myteamEventsListener) {
    this.eventPublisher = eventPublisher;
    this.groupManager = groupManager;
    this.notificationFilterManager = notificationFilterManager;
    this.notificationSchemeManager = notificationSchemeManager;
    this.permissionManager = permissionManager;
    this.projectRoleManager = projectRoleManager;
    this.userData = userData;
    this.messageFormatter = messageFormatter;
    this.myteamEventsListener = myteamEventsListener;
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

        Set<NotificationRecipient> notificationRecipients =
            notificationSchemeManager.getRecipients(issueEvent);
        NotificationFilterContext context =
            notificationFilterManager.makeContextFrom(
                JiraNotificationReason.ISSUE_EVENT, issueEvent);
        for (SchemeEntity schemeEntity :
            notificationSchemeManager.getNotificationSchemeEntities(
                issueEvent.getProject(), issueEvent.getEventTypeId())) {
          context =
              notificationFilterManager.makeContextFrom(
                  context,
                  com.atlassian.jira.notification.type.NotificationType.from(
                      schemeEntity.getType()));
          Set<NotificationRecipient> recipientsFromScheme =
              notificationSchemeManager.getRecipients(issueEvent, schemeEntity);
          recipientsFromScheme =
              Sets.newHashSet(
                  notificationFilterManager.recomputeRecipients(recipientsFromScheme, context));
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
    if (groupName != null && !groupManager.isUserInGroup(user, groupName)) return false;
    if (projectRole != null
        && !projectRoleManager.isUserInProjectRole(user, projectRole, issue.getProjectObject()))
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
            myteamEventsListener.publishEvent(
                new JiraNotifyEvent(
                    mrimLogin, message, messageFormatter.getAllIssueButtons(issueKey, recipient)));
          } else {
            myteamEventsListener.publishEvent(new JiraNotifyEvent(mrimLogin, message, null));
          }
        }
      }
    }
  }
}
