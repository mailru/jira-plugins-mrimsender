/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.remotelink;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import lombok.Getter;

@Getter
public class RemoteIssueLinkData {
  private final Issue issue;
  private final ApplicationUser linkCreator;

  private RemoteIssueLinkData(final Issue issue, final ApplicationUser linkCreator) {
    this.issue = issue;
    this.linkCreator = linkCreator;
  }

  public static RemoteIssueLinkData of(final Issue issue, final ApplicationUser linkCreator) {
    return new RemoteIssueLinkData(issue, linkCreator);
  }
}
