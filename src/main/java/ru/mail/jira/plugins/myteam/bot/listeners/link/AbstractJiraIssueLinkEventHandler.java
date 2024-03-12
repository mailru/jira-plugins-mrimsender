/* (C)2024 */
package ru.mail.jira.plugins.myteam.bot.listeners.link;

import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.security.JiraAuthenticationContext;
import java.util.Map;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.component.event.link.IssueLinkEventData;
import ru.mail.jira.plugins.myteam.component.event.link.IssueLinkEventRecipientResolver;
import ru.mail.jira.plugins.myteam.component.event.link.IssueLinkEventRecipientsData;
import ru.mail.jira.plugins.myteam.component.event.link.IssueLinkEventToVKTeamSender;

public abstract class AbstractJiraIssueLinkEventHandler {

  protected final JiraAuthenticationContext jiraAuthenticationContext;
  private final IssueLinkEventRecipientResolver issueLinkEventRecipientResolver;
  private final IssueLinkEventToVKTeamSender issueLinkEventToVKTeamSender;

  protected AbstractJiraIssueLinkEventHandler(
      final JiraAuthenticationContext jiraAuthenticationContext,
      final IssueLinkEventRecipientResolver issueLinkEventRecipientResolver,
      final IssueLinkEventToVKTeamSender issueLinkEventToVKTeamSender) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.issueLinkEventRecipientResolver = issueLinkEventRecipientResolver;
    this.issueLinkEventToVKTeamSender = issueLinkEventToVKTeamSender;
  }

  protected void handle(final IssueLink issueLink, final boolean linkCreated) {
    if (!jiraAuthenticationContext.isLoggedInUser()) {
      return;
    }

    final IssueLinkEventRecipientsData recipientsData =
        issueLinkEventRecipientResolver.resolve(
            IssueLinkEventData.of(
                issueLink, jiraAuthenticationContext.getLoggedInUser(), linkCreated));
    try {
      issueLinkEventToVKTeamSender.send(recipientsData);
    } catch (Exception e) {
      SentryClient.capture(
          e,
          Map.of(
              "issueLinkTypeId", String.valueOf(issueLink.getIssueLinkType().getId()),
              "issueLinkTypeName", issueLink.getIssueLinkType().getName(),
              "sourceIssue", issueLink.getSourceObject().getKey(),
              "destinationIssue", issueLink.getDestinationObject().getKey()));
    }
  }
}
