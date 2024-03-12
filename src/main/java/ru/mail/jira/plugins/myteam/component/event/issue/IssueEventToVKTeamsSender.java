/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.issue;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.bot.listeners.IssueEventRecipient;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.component.JiraIssueEventToChatMessageConverter;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.component.event.AbstractChatMessageSender;
import ru.mail.jira.plugins.myteam.component.event.IssueEventChatMessageButtonBuilder;

@Component
public class IssueEventToVKTeamsSender
    extends AbstractChatMessageSender<IssueEventData, IssueEventRecipient> {
  private final JiraIssueEventToChatMessageConverter jiraIssueEventToChatMessageConverter;

  @Autowired
  public IssueEventToVKTeamsSender(
      @ComponentImport final JiraAuthenticationContext jiraAuthenticationContext,
      final MyteamEventsListener myteamEventsListener,
      final UserData userData,
      final IssueEventChatMessageButtonBuilder issueEventChatMessageButtonBuilder,
      final JiraIssueEventToChatMessageConverter jiraIssueEventToChatMessageConverter) {
    super(
        jiraAuthenticationContext,
        myteamEventsListener,
        userData,
        issueEventChatMessageButtonBuilder);
    this.jiraIssueEventToChatMessageConverter = jiraIssueEventToChatMessageConverter;
  }

  @Override
  public void send(final IssueEventData issueEventData) {
    if (issueEventData.getIssueEventRecipients().isEmpty()) {
      return;
    }

    super.send(
        issueEventData.getIssueEventRecipients(),
        eventRecipient ->
            jiraIssueEventToChatMessageConverter.convert(
                IssueEventToChatMessageData.of(eventRecipient, issueEventData.getIssueEvent())),
        issueEventData.getIssueEvent().getIssue().getKey());
  }

  @Override
  protected boolean needButtons() {
    return true;
  }
}
