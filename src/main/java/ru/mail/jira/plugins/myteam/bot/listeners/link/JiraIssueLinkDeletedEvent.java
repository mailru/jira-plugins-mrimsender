/* (C)2024 */
package ru.mail.jira.plugins.myteam.bot.listeners.link;

import com.atlassian.event.api.EventListener;
import com.atlassian.jira.event.issue.link.IssueLinkDeletedEvent;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.bot.listeners.IEventListener;
import ru.mail.jira.plugins.myteam.component.event.link.IssueLinkEventRecipientResolver;
import ru.mail.jira.plugins.myteam.component.event.link.IssueLinkEventToVKTeamSender;

@Component
public class JiraIssueLinkDeletedEvent extends AbstractJiraIssueLinkEventHandler
    implements IEventListener {

  public JiraIssueLinkDeletedEvent(
      @ComponentImport final JiraAuthenticationContext jiraAuthenticationContext,
      final IssueLinkEventRecipientResolver issueLinkEventRecipientResolver,
      final IssueLinkEventToVKTeamSender issueLinkEventToVKTeamSender) {
    super(jiraAuthenticationContext, issueLinkEventRecipientResolver, issueLinkEventToVKTeamSender);
  }

  @EventListener
  public void onEvent(IssueLinkDeletedEvent issueLinkDeletedEvent) {
    super.handle(issueLinkDeletedEvent.getIssueLink(), false);
  }
}
