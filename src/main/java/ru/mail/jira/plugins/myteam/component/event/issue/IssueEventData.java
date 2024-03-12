/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.issue;

import com.atlassian.jira.event.issue.IssueEvent;
import java.util.Set;
import lombok.Getter;
import ru.mail.jira.plugins.myteam.bot.listeners.IssueEventRecipient;

@Getter
public class IssueEventData {

  private final Set<IssueEventRecipient> issueEventRecipients;
  private final IssueEvent issueEvent;

  private IssueEventData(
      final Set<IssueEventRecipient> issueEventRecipients, final IssueEvent issueEvent) {
    this.issueEventRecipients = issueEventRecipients;
    this.issueEvent = issueEvent;
  }

  public static IssueEventData of(
      final Set<IssueEventRecipient> issueEventRecipients, final IssueEvent issueEvent) {
    return new IssueEventData(issueEventRecipients, issueEvent);
  }
}
