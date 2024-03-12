/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.remotelink;

import com.atlassian.jira.user.ApplicationUser;
import java.util.Set;
import lombok.Getter;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;

@Getter
public class RemoteIssueLinkRecipientsData {
  private final String linkTitle;
  private final String linkUrl;
  private final String issueKey;
  private final ApplicationUser linkCreator;
  private final Set<EventRecipient> eventRecipients;

  private RemoteIssueLinkRecipientsData(
      final String linkTitle,
      final String linkUrl,
      final String issueKey,
      final ApplicationUser linkCreator,
      final Set<EventRecipient> eventRecipients) {
    this.linkTitle = linkTitle;
    this.linkUrl = linkUrl;
    this.issueKey = issueKey;
    this.linkCreator = linkCreator;
    this.eventRecipients = eventRecipients;
  }

  public static RemoteIssueLinkRecipientsData of(
      final String linkTitle,
      final String linkUrl,
      final String issueKey,
      final ApplicationUser linkCreator,
      final Set<EventRecipient> eventRecipients) {
    return new RemoteIssueLinkRecipientsData(
        linkTitle, linkUrl, issueKey, linkCreator, eventRecipients);
  }
}
