/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.link;

import com.atlassian.jira.user.ApplicationUser;
import lombok.Getter;

@Getter
public class IssueLinkEventToChatMessageData {
  private final String issueKeySource;
  private final String issueKeyDestination;
  private final String sourceIssueSummary;
  private final String destinationIssueSummary;
  private final ApplicationUser eventCreator;
  private final String linkTypeName;
  private final boolean linkCreated;

  public IssueLinkEventToChatMessageData(
      final String issueKeySource,
      final String issueKeyDestination,
      final String sourceIssueSummary,
      final String destinationIssueSummary,
      final ApplicationUser eventCreator,
      final String linkTypeName,
      final boolean linkCreated) {
    this.issueKeySource = issueKeySource;
    this.issueKeyDestination = issueKeyDestination;
    this.sourceIssueSummary = sourceIssueSummary;
    this.destinationIssueSummary = destinationIssueSummary;
    this.eventCreator = eventCreator;
    this.linkTypeName = linkTypeName;
    this.linkCreated = linkCreated;
  }

  public static IssueLinkEventToChatMessageData of(
      final String issueKeySource,
      final String issueKeyDestination,
      final String sourceIssueSummary,
      final String destinationIssueSummary,
      final ApplicationUser eventCreator,
      final String linkTypeName,
      final boolean linkCreated) {
    return new IssueLinkEventToChatMessageData(
        issueKeySource,
        issueKeyDestination,
        sourceIssueSummary,
        destinationIssueSummary,
        eventCreator,
        linkTypeName,
        linkCreated);
  }
}
