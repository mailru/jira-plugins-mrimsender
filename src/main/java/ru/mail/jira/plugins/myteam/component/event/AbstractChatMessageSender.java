/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.events.JiraNotifyEvent;
import ru.mail.jira.plugins.myteam.bot.listeners.MyteamEventsListener;
import ru.mail.jira.plugins.myteam.component.UserData;

@Slf4j
public abstract class AbstractChatMessageSender<T, R extends EventRecipient>
    implements JiraEventToVKTeamSender<T> {

  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final MyteamEventsListener myteamEventsListener;
  private final UserData userData;
  private final IssueEventChatMessageButtonBuilder issueEventChatMessageButtonBuilder;

  protected AbstractChatMessageSender(
      final JiraAuthenticationContext jiraAuthenticationContext,
      final MyteamEventsListener myteamEventsListener,
      final UserData userData,
      final IssueEventChatMessageButtonBuilder issueEventChatMessageButtonBuilder) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.myteamEventsListener = myteamEventsListener;
    this.userData = userData;
    this.issueEventChatMessageButtonBuilder = issueEventChatMessageButtonBuilder;
  }

  protected void send(
      final Set<R> eventRecipients,
      final Function<R, String> messageProvider,
      final String issueKey) {
    for (final R eventRecipient : eventRecipients) {
      final ApplicationUser recipient = eventRecipient.getRecipient();
      if (recipient.isActive() && userData.isEnabled(recipient)) {
        if (StringUtils.isBlank(recipient.getEmailAddress())) {
          continue;
        }

        final ApplicationUser contextUser = jiraAuthenticationContext.getLoggedInUser();
        jiraAuthenticationContext.setLoggedInUser(recipient);
        final String message = messageProvider.apply(eventRecipient);
        if (StringUtils.isBlank(message)) {
          continue;
        }

        try {
          myteamEventsListener.publishEvent(
              new JiraNotifyEvent(
                  recipient.getEmailAddress(),
                  message,
                  this.needButtons() ? issueEventChatMessageButtonBuilder.build(issueKey) : null));
        } catch (final Exception e) {
          log.error("Error happened during send message to user {}", recipient.getEmailAddress());
          SentryClient.capture(e, Map.of("issueKey", issueKey, "recipient", recipient.getKey()));
        } finally {
          jiraAuthenticationContext.setLoggedInUser(contextUser);
        }
      }
    }
  }

  protected abstract boolean needButtons();
}
