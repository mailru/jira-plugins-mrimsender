/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.link;

import com.atlassian.jira.user.ApplicationUser;
import java.util.Collections;
import java.util.Set;
import lombok.Getter;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;

@Getter
public class IssueLinkEventRecipientsData {
  private final Set<EventRecipient> eventRecipientsForSourceIssue;
  private final String sourceIssueKey;
  private final String sourceIssueSummary;
  private final Set<EventRecipient> eventRecipientsForDestinationIssue;
  private final String destinationIssueKey;
  private final String destinationIssueSummary;
  private final ApplicationUser issueLinkCreatorOrRemover;
  private final String linkTypeName;
  private final boolean linkCreated;

  private IssueLinkEventRecipientsData(
      final Set<EventRecipient> eventRecipientsForSourceIssue,
      final String sourceIssueKey,
      final String sourceIssueSummary,
      final Set<EventRecipient> eventRecipientsForDestinationIssue,
      final String destinationIssueKey,
      final String destinationIssueSummary,
      final ApplicationUser issueLinkCreatorOrRemover,
      final String linkTypeName,
      final boolean linkCreated) {
    this.eventRecipientsForSourceIssue = eventRecipientsForSourceIssue;
    this.sourceIssueKey = sourceIssueKey;
    this.sourceIssueSummary = sourceIssueSummary;
    this.eventRecipientsForDestinationIssue = eventRecipientsForDestinationIssue;
    this.destinationIssueKey = destinationIssueKey;
    this.destinationIssueSummary = destinationIssueSummary;
    this.issueLinkCreatorOrRemover = issueLinkCreatorOrRemover;
    this.linkTypeName = linkTypeName;
    this.linkCreated = linkCreated;
  }

  public static IssueLinkEventRecipientsData of(
      final Set<EventRecipient> eventRecipientsForSourceIssue,
      final String sourceIssueKey,
      final String sourceIssueSummary,
      final Set<EventRecipient> eventRecipientsForDestinationIssue,
      final String destinationIssueKey,
      final String destinationIssueSummary,
      final ApplicationUser issueLinkCreator,
      final String linkTypeName,
      final boolean linkCreated) {
    return new IssueLinkEventRecipientsData(
        Collections.unmodifiableSet(eventRecipientsForSourceIssue),
        sourceIssueKey,
        sourceIssueSummary,
        Collections.unmodifiableSet(eventRecipientsForDestinationIssue),
        destinationIssueKey,
        destinationIssueSummary,
        issueLinkCreator,
        linkTypeName,
        linkCreated);
  }
}
