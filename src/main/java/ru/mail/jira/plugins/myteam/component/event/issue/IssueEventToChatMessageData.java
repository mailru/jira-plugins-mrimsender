/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.issue;

import com.atlassian.jira.event.issue.IssueEvent;
import lombok.Getter;

@Getter
public class IssueEventToChatMessageData {
  private final IssueEventRecipient issueEventRecipient;
  private final IssueEvent issueEvent;

  private IssueEventToChatMessageData(
      final IssueEventRecipient issueEventRecipient, final IssueEvent issueEvent) {
    this.issueEventRecipient = issueEventRecipient;
    this.issueEvent = issueEvent;
  }

  public static IssueEventToChatMessageData of(
      final IssueEventRecipient issueEventRecipient, final IssueEvent issueEvent) {
    return new IssueEventToChatMessageData(issueEventRecipient, issueEvent);
  }
}
