/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.link;

import com.atlassian.jira.issue.link.IssueLink;
import com.atlassian.jira.user.ApplicationUser;
import lombok.Getter;

@Getter
public class IssueLinkEventData {
  private final IssueLink issueLink;
  private final ApplicationUser issueLinkCreatorOrRemover;
  private final boolean linkCreated;

  private IssueLinkEventData(
      final IssueLink issueLink,
      final ApplicationUser issueLinkCreatorOrRemover,
      final boolean linkCreated) {
    this.issueLink = issueLink;
    this.issueLinkCreatorOrRemover = issueLinkCreatorOrRemover;
    this.linkCreated = linkCreated;
  }

  public static IssueLinkEventData of(
      final IssueLink issueLink, final ApplicationUser applicationUser, final boolean linkCreated) {
    return new IssueLinkEventData(issueLink, applicationUser, linkCreated);
  }
}
