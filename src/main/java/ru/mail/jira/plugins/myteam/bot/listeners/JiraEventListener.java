/* (C)2020 */
package ru.mail.jira.plugins.myteam.bot.listeners;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.MentionIssueEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.notification.*;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.scheme.SchemeEntity;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JiraEventListener implements InitializingBean, DisposableBean {
  private final EventPublisher eventPublisher;
  private final GroupManager groupManager;
  private final NotificationFilterManager notificationFilterManager;
  private final NotificationSchemeManager notificationSchemeManager;
  private final PermissionManager permissionManager;
  private final ProjectRoleManager projectRoleManager;
  private final UserData userData;
  private final MessageFormatter messageFormatter;
  private final MyteamEventsListener myteamEventsListener;
  private final I18nResolver i18nResolver;
  private final JiraAuthenticationContext jiraAuthenticationContext;

  @Autowired
  public JiraEventListener(
      @ComponentImport EventPublisher eventPublisher,
      @ComponentImport GroupManager groupManager,
      @ComponentImport NotificationFilterManager notificationFilterManager,
      @ComponentImport NotificationSchemeManager notificationSchemeManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport ProjectRoleManager projectRoleManager,
      @ComponentImport I18nResolver i18nResolver,
      UserData userData,
      MessageFormatter messageFormatter,
      MyteamEventsListener myteamEventsListener,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
    this.eventPublisher = eventPublisher;
    this.groupManager = groupManager;
    this.notificationFilterManager = notificationFilterManager;
    this.notificationSchemeManager = notificationSchemeManager;
    this.permissionManager = permissionManager;
    this.projectRoleManager = projectRoleManager;
    this.userData = userData;
    this.messageFormatter = messageFormatter;
    this.myteamEventsListener = myteamEventsListener;
    this.i18nResolver = i18nResolver;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
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

        Set<NotificationRecipient> notificationRecipients = Sets.newHashSet();
        try {
          notificationRecipients.addAll(notificationSchemeManager.getRecipients(issueEvent));
        } catch (Exception e) {
          log.error("notificationSchemeManager.getRecipients({})", issueEvent, e);
        }
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

        Set<ApplicationUser> recipients =
            notificationRecipients.stream()
                .map(NotificationRecipient::getUser)
                .filter(user -> canSendEventToUser(user, issueEvent))
                .collect(Collectors.toSet());

        sendMessage(recipients, issueEvent, issueEvent.getIssue().getKey());
      }
    } catch (Exception e) {
      SentryClient.capture(e);
      log.error("onIssueEvent({})", issueEvent, e);
    }
  }

  @SuppressWarnings("unused")
  @EventListener
  public void onMentionIssueEvent(MentionIssueEvent mentionIssueEvent) {
    try {
      List<ApplicationUser> recipients = new ArrayList<>();
      for (ApplicationUser user : mentionIssueEvent.getToUsers()) {
        if (mentionIssueEvent.getCurrentRecipients() != null
            && !mentionIssueEvent
                .getCurrentRecipients()
                .contains(new NotificationRecipient(user))) {
          recipients.add(user);
        }
      }
      sendMessage(recipients, mentionIssueEvent, mentionIssueEvent.getIssue().getKey());
    } catch (Exception e) {
      SentryClient.capture(e);
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

    if (!permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user)) {
      return false;
    }
    if (groupName != null && !groupManager.isUserInGroup(user, groupName)) {
      return false;
    }
    return projectRole == null
        || projectRoleManager.isUserInProjectRole(user, projectRole, issue.getProjectObject());
  }

  private void sendMessage(Collection<ApplicationUser> recipients, Object event, String issueKey) {
    for (ApplicationUser recipient : recipients) {
      if (recipient.isActive() && userData.isEnabled(recipient)) {
        if (StringUtils.isNotBlank(recipient.getEmailAddress())) {

          ApplicationUser contextUser = jiraAuthenticationContext.getLoggedInUser();
          jiraAuthenticationContext.setLoggedInUser(recipient);
          try {
            String message = null;
            if (event instanceof IssueEvent)
              message = messageFormatter.formatEvent(recipient, (IssueEvent) event);
            if (event instanceof MentionIssueEvent)
              message = messageFormatter.formatEvent(recipient, (MentionIssueEvent) event);
            if (message != null) {
              myteamEventsListener.publishEvent(
                  new JiraNotifyEvent(
                      recipient.getEmailAddress(), message, getAllIssueButtons(issueKey)));
            } else {
              myteamEventsListener.publishEvent(
                  new JiraNotifyEvent(recipient.getEmailAddress(), message, null));
            }
          } catch (Exception e) {
            SentryClient.capture(event.toString());
            SentryClient.capture(e);
          } finally {
            jiraAuthenticationContext.setLoggedInUser(contextUser);
          }
        }
      }
    }
  }

  private List<List<InlineKeyboardMarkupButton>> getAllIssueButtons(String issueKey) {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>();
    List<InlineKeyboardMarkupButton> buttonsRow = new ArrayList<>();
    buttons.add(buttonsRow);

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.commentButton.text"),
            String.join("-", ButtonRuleType.CommentIssue.getName(), issueKey)));

    buttonsRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.showCommentsButton.text"),
            String.join("-", ButtonRuleType.ViewComments.getName(), issueKey)));

    ArrayList<InlineKeyboardMarkupButton> assignAndTransitionButtonRow = new ArrayList<>();

    assignAndTransitionButtonRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.assign.text"),
            String.join("-", CommandRuleType.AssignIssue.getName(), issueKey)));
    assignAndTransitionButtonRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.messageFormatter.editIssue.transitionChange.title"),
            String.join("-", CommandRuleType.IssueTransition.getName(), issueKey)));

    buttons.add(assignAndTransitionButtonRow);

    List<InlineKeyboardMarkupButton> quickViewAndMenuRow = new ArrayList<>();
    quickViewAndMenuRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getRawText(
                "ru.mail.jira.plugins.myteam.mrimsenderEventListener.quickViewButton.text"),
            String.join("-", CommandRuleType.Issue.getName(), issueKey)));
    quickViewAndMenuRow.add(
        InlineKeyboardMarkupButton.buildButtonWithoutUrl(
            i18nResolver.getText("ru.mail.jira.plugins.myteam.messageQueueProcessor.mainMenu.text"),
            CommandRuleType.Menu.getName()));
    buttons.add(quickViewAndMenuRow);

    return buttons;
  }
}
