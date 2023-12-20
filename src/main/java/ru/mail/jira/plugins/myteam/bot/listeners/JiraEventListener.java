/* (C)2020 */
package ru.mail.jira.plugins.myteam.bot.listeners;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.ButtonRuleType;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.ruletypes.CommandRuleType;
import ru.mail.jira.plugins.myteam.component.JiraEventToChatMessageConverter;
import ru.mail.jira.plugins.myteam.component.PluginMentionService;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;

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

  private final MyteamEventsListener myteamEventsListener;
  private final I18nResolver i18nResolver;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final JiraEventToChatMessageConverter jiraEventToChatMessageConverter;

  private final PluginMentionService pluginMentionService;

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
      MyteamEventsListener myteamEventsListener,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      JiraEventToChatMessageConverter jiraEventToChatMessageConverter,
      PluginMentionService pluginMentionService) {
    this.eventPublisher = eventPublisher;
    this.groupManager = groupManager;
    this.notificationFilterManager = notificationFilterManager;
    this.notificationSchemeManager = notificationSchemeManager;
    this.permissionManager = permissionManager;
    this.projectRoleManager = projectRoleManager;
    this.userData = userData;
    this.myteamEventsListener = myteamEventsListener;
    this.i18nResolver = i18nResolver;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.jiraEventToChatMessageConverter = jiraEventToChatMessageConverter;
    this.pluginMentionService = pluginMentionService;
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

        Set<ApplicationUser> mentionedUsers = resolveMentionedUsersInIssueEvent(issueEvent);

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

          Set<NotificationRecipient> recipientsFromScheme = new HashSet<>();

          try {
            recipientsFromScheme =
                notificationSchemeManager.getRecipients(issueEvent, schemeEntity);
          } catch (NullPointerException e) {
            log.error(e.getLocalizedMessage(), e);
            SentryClient.capture(
                e,
                Map.of(
                    "error",
                    "Error while retrieving issue notification recipients. (Possibly related to components)",
                    "issueKey",
                    issueEvent.getIssue().getKey()));
          }

          recipientsFromScheme =
              Sets.newHashSet(
                  notificationFilterManager.recomputeRecipients(recipientsFromScheme, context));
          notificationRecipients.addAll(recipientsFromScheme);
        }

        Set<MentionedApplicationUser> mentionedApplicationUsers =
            getRecipientFilteredByPermissionsAndMentions(
                issueEvent, mentionedUsers, notificationRecipients);

        sendMessage(mentionedApplicationUsers, issueEvent, issueEvent.getIssue().getKey());
      }
    } catch (Exception e) {
      SentryClient.capture(e, Map.of("issueKey", issueEvent.getIssue().getKey()));
      log.error("onIssueEvent({})", issueEvent, e);
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

  private void sendMessage(
      Collection<MentionedApplicationUser> recipients, IssueEvent event, String issueKey) {
    for (MentionedApplicationUser mentionedApplicationUser : recipients) {
      ApplicationUser recipient = mentionedApplicationUser.getApplicationUser();
      if (recipient.isActive() && userData.isEnabled(recipient)) {
        if (StringUtils.isNotBlank(recipient.getEmailAddress())) {

          ApplicationUser contextUser = jiraAuthenticationContext.getLoggedInUser();
          jiraAuthenticationContext.setLoggedInUser(recipient);
          try {
            String message =
                jiraEventToChatMessageConverter.formatEventWithDiff(
                    mentionedApplicationUser, event);
            if (message != null) {
              myteamEventsListener.publishEvent(
                  new JiraNotifyEvent(
                      recipient.getEmailAddress(), message, getAllIssueButtons(issueKey)));
            }
          } catch (Exception e) {
            SentryClient.capture(e, Map.of("issueKey", issueKey, "recipient", recipient.getKey()));
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

  @NotNull
  private Set<ApplicationUser> resolveMentionedUsersInIssueEvent(
      @NotNull final IssueEvent issueEvent) {
    Long eventTypeId = issueEvent.getEventTypeId();
    Set<ApplicationUser> mentionedUsers;
    if (EventType.ISSUE_UPDATED_ID.equals(eventTypeId)) {
      mentionedUsers =
          pluginMentionService.getMentionedUsersInDescription(issueEvent.getIssue(), true);
    } else if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)) {
      mentionedUsers = pluginMentionService.getMentionedUserInComment(issueEvent.getComment());
    } else if (EventType.ISSUE_COMMENT_EDITED_ID.equals(eventTypeId)) {
      Object origCommentObject =
          issueEvent.getParams().get(CommentManager.EVENT_ORIGINAL_COMMENT_PARAMETER);
      if (origCommentObject instanceof Comment) {
        mentionedUsers =
            pluginMentionService.getMentionedUserInEditedComment(
                issueEvent.getComment(), (Comment) origCommentObject);
      } else {
        mentionedUsers = Collections.emptySet();
      }
    } else {
      mentionedUsers = Collections.emptySet();
    }
    return mentionedUsers;
  }

  @NotNull
  private Set<MentionedApplicationUser> getRecipientFilteredByPermissionsAndMentions(
      @NotNull final IssueEvent issueEvent,
      @NotNull final Set<ApplicationUser> mentionedUsers,
      @NotNull final Set<NotificationRecipient> notificationRecipients) {
    Map<String, ApplicationUser> mentionedUsersMap =
        mentionedUsers.stream()
            .filter(mentionedUser -> canSendEventToUser(mentionedUser, issueEvent))
            .collect(Collectors.toMap(ApplicationUser::getKey, Function.identity()));

    Set<ApplicationUser> recipients =
        notificationRecipients.stream()
            .map(NotificationRecipient::getUser)
            .filter(user -> canSendEventToUser(user, issueEvent))
            .collect(Collectors.toSet());

    Set<MentionedApplicationUser> mentionedApplicationUsers = new HashSet<>();
    for (ApplicationUser recipient : recipients) {
      ApplicationUser mentionedUser = mentionedUsersMap.remove(recipient.getKey());
      if (mentionedUser == null) {
        mentionedApplicationUsers.add(MentionedApplicationUser.notMentionedUser(recipient));
      } else {
        mentionedApplicationUsers.add(MentionedApplicationUser.mentionedUser(recipient));
      }
    }

    mentionedApplicationUsers.addAll(
        mentionedUsersMap.values().stream()
            .map(MentionedApplicationUser::mentionedUser)
            .collect(Collectors.toSet()));
    return mentionedApplicationUsers;
  }
}
