/* (C)2024 */
package ru.mail.jira.plugins.myteam.bot.listeners.remotelink;

import com.atlassian.event.api.EventListener;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.link.RemoteIssueLinkService;
import com.atlassian.jira.event.issue.link.RemoteIssueLinkUICreateEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.RemoteIssueLink;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.listeners.IEventListener;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;
import ru.mail.jira.plugins.myteam.component.event.remotelink.RemoteIssueLinkData;
import ru.mail.jira.plugins.myteam.component.event.remotelink.RemoteIssueLinkEventRecipientResolver;
import ru.mail.jira.plugins.myteam.component.event.remotelink.RemoteIssueLinkRecipientsData;
import ru.mail.jira.plugins.myteam.component.event.remotelink.RemoteIssueLinkToVKTeamSender;

@Component
public class RemoteIssueLinkCreatedEventListener implements IEventListener {
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final RemoteIssueLinkService remoteIssueLinkService;
  private final IssueService issueService;
  private final RemoteIssueLinkEventRecipientResolver remoteIssueLinkEventRecipientResolver;
  private final RemoteIssueLinkToVKTeamSender remoteIssueLinkToVKTeamSender;

  @Autowired
  public RemoteIssueLinkCreatedEventListener(
      @ComponentImport final JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport final RemoteIssueLinkService remoteIssueLinkService,
      @ComponentImport final IssueService issueService,
      final RemoteIssueLinkEventRecipientResolver remoteIssueLinkEventRecipientResolver,
      final RemoteIssueLinkToVKTeamSender remoteIssueLinkToVKTeamSender) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.remoteIssueLinkService = remoteIssueLinkService;
    this.issueService = issueService;
    this.remoteIssueLinkEventRecipientResolver = remoteIssueLinkEventRecipientResolver;
    this.remoteIssueLinkToVKTeamSender = remoteIssueLinkToVKTeamSender;
  }

  // https://jira.atlassian.com/browse/JRASERVER-45989
  @EventListener
  public void onEvent(final RemoteIssueLinkUICreateEvent remoteIssueLinkCreateEvent) {
    final ValidationResult validationResult = validateEventData(remoteIssueLinkCreateEvent);
    if (validationResult == null) {
      return;
    }

    handle(validationResult);
  }

  private void handle(final ValidationResult validationResult) {
    final RemoteIssueLink remoteIssueLink =
        validationResult.remoteIssueLinkValidationResult.getRemoteIssueLink();
    final Issue issue = validationResult.issueValidationResult.getIssue();

    final Set<EventRecipient> recipients =
        remoteIssueLinkEventRecipientResolver.resolve(
            RemoteIssueLinkData.of(issue, jiraAuthenticationContext.getLoggedInUser()));
    if (recipients.isEmpty()) {
      return;
    }

    final RemoteIssueLinkRecipientsData remoteIssueLinkRecipientsData =
        RemoteIssueLinkRecipientsData.of(
            StringUtils.defaultString(remoteIssueLink.getTitle()),
            remoteIssueLink.getUrl(),
            issue.getKey(),
            issue.getSummary(),
            jiraAuthenticationContext.getLoggedInUser(),
            recipients);

    try {
      remoteIssueLinkToVKTeamSender.send(remoteIssueLinkRecipientsData);
    } catch (Exception e) {
      SentryClient.capture(
          e,
          Map.of("issueKey", remoteIssueLinkRecipientsData.getIssueKey()),
          Map.of(
              "linkTitle",
              remoteIssueLinkRecipientsData.getLinkTitle(),
              "linkUrl",
              remoteIssueLinkRecipientsData.getLinkUrl()));
    }
  }

  @Nullable
  private RemoteIssueLinkCreatedEventListener.@Nullable ValidationResult validateEventData(
      RemoteIssueLinkUICreateEvent remoteIssueLinkCreateEvent) {
    if (!jiraAuthenticationContext.isLoggedInUser()) {
      return null;
    }

    final ApplicationUser linkCreator = jiraAuthenticationContext.getLoggedInUser();

    final RemoteIssueLinkService.RemoteIssueLinkResult remoteIssueLinkValidationResult =
        remoteIssueLinkService.getRemoteIssueLink(
            linkCreator, remoteIssueLinkCreateEvent.getRemoteIssueLinkId());
    if (!remoteIssueLinkValidationResult.isValid()) {
      return null;
    }

    final IssueService.IssueResult issueValidationResult =
        issueService.getIssue(
            linkCreator, remoteIssueLinkValidationResult.getRemoteIssueLink().getIssueId());
    if (!issueValidationResult.isValid()) {
      return null;
    }

    return new ValidationResult(remoteIssueLinkValidationResult, issueValidationResult);
  }

  private static final class ValidationResult {
    public final RemoteIssueLinkService.RemoteIssueLinkResult remoteIssueLinkValidationResult;
    public final IssueService.IssueResult issueValidationResult;

    public ValidationResult(
        final RemoteIssueLinkService.RemoteIssueLinkResult remoteIssueLinkValidationResult,
        final IssueService.IssueResult issueValidationResult) {
      this.remoteIssueLinkValidationResult = remoteIssueLinkValidationResult;
      this.issueValidationResult = issueValidationResult;
    }
  }
}
