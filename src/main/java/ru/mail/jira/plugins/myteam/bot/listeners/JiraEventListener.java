/* (C)2020 */
package ru.mail.jira.plugins.myteam.bot.listeners;

import com.atlassian.event.api.EventListener;
import com.atlassian.jira.event.issue.IssueEvent;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.component.event.issue.IssueEventData;
import ru.mail.jira.plugins.myteam.component.event.issue.IssueEventRecipientResolver;
import ru.mail.jira.plugins.myteam.component.event.issue.IssueEventToVKTeamsSender;

@Component
@Slf4j
public class JiraEventListener implements IEventListener {
  private final IssueEventRecipientResolver issueEventRecipientResolver;
  private final IssueEventToVKTeamsSender issueEventToVKTeamsSender;

  @Autowired
  public JiraEventListener(
      final IssueEventRecipientResolver issueEventRecipientResolver,
      final IssueEventToVKTeamsSender issueEventToVKTeamsSender) {
    this.issueEventRecipientResolver = issueEventRecipientResolver;
    this.issueEventToVKTeamsSender = issueEventToVKTeamsSender;
  }

  @SuppressWarnings("unused")
  @EventListener
  public void onIssueEvent(final IssueEvent issueEvent) {
    if (!issueEvent.isSendMail()) {
      return;
    }

    handle(issueEvent);
  }

  private void handle(final IssueEvent issueEvent) {
    final Set<IssueEventRecipient> recipients = issueEventRecipientResolver.resolve(issueEvent);
    try {
      issueEventToVKTeamsSender.send(IssueEventData.of(recipients, issueEvent));
    } catch (Exception e) {
      SentryClient.capture(e, Map.of("issueKey", issueEvent.getIssue().getKey()));
      log.error("onIssueEvent({})", issueEvent, e);
    }
  }
}
