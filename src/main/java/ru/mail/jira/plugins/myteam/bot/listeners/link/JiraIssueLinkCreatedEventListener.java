/* (C)2024 */
package ru.mail.jira.plugins.myteam.bot.listeners.link;

import com.atlassian.event.api.EventListener;
import com.atlassian.jira.event.issue.link.IssueLinkCreatedEvent;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.bot.listeners.IEventListener;
import ru.mail.jira.plugins.myteam.component.event.link.*;

@Component
public class JiraIssueLinkCreatedEventListener extends AbstractJiraIssueLinkEventHandler
    implements IEventListener {

  public JiraIssueLinkCreatedEventListener(
      @ComponentImport final JiraAuthenticationContext jiraAuthenticationContext,
      final IssueLinkEventRecipientResolver issueLinkEventRecipientResolver,
      final IssueLinkEventToVKTeamSender issueLinkEventToVKTeamSender) {
    super(jiraAuthenticationContext, issueLinkEventRecipientResolver, issueLinkEventToVKTeamSender);
  }

  @EventListener
  public void onEvent(final IssueLinkCreatedEvent issueLinkCreatedEvent) {
    super.handle(issueLinkCreatedEvent.getIssueLink(), true);
  }
}
