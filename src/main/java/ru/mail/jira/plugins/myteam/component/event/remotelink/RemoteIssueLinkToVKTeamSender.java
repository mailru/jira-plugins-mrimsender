/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.remotelink;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.component.event.AbstractChatMessageSender;
import ru.mail.jira.plugins.myteam.component.event.EventRecipient;
import ru.mail.jira.plugins.myteam.component.event.IssueEventChatMessageButtonBuilder;

@Component
public class RemoteIssueLinkToVKTeamSender
    extends AbstractChatMessageSender<RemoteIssueLinkRecipientsData, EventRecipient> {
  private final RemoteIssueLinkToChatMessageConverter remoteIssueLinkToChatMessageConverter;

  @Autowired
  public RemoteIssueLinkToVKTeamSender(
      @ComponentImport final JiraAuthenticationContext jiraAuthenticationContext,
      final MyteamEventsListener myteamEventsListener,
      final UserData userData,
      final IssueEventChatMessageButtonBuilder issueEventChatMessageButtonBuilder,
      final RemoteIssueLinkToChatMessageConverter remoteIssueLinkToChatMessageConverter) {
    super(
        jiraAuthenticationContext,
        myteamEventsListener,
        userData,
        issueEventChatMessageButtonBuilder);
    this.remoteIssueLinkToChatMessageConverter = remoteIssueLinkToChatMessageConverter;
  }

  @Override
  public void send(final RemoteIssueLinkRecipientsData remoteIssueLinkRecipientsData) {
    super.send(
        remoteIssueLinkRecipientsData.getEventRecipients(),
        eventRecipient ->
            remoteIssueLinkToChatMessageConverter.convert(remoteIssueLinkRecipientsData),
        remoteIssueLinkRecipientsData.getIssueKey());
  }

  @Override
  protected boolean needButtons() {
    return true;
  }
}
