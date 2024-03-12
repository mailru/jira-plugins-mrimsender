/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.link;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.component.event.JiraEventToChatMessageConverter;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;

@Component
public class JiraIssueLinkToChatMessageConverter
    implements JiraEventToChatMessageConverter<IssueLinkEventToChatMessageData> {
  private static final String I18N_ISSUE_LINK_CREATED_EVENT_KEY =
      "ru.mail.jira.plugins.myteam.notification.issue.link.created";
  private static final String I18N_ISSUE_LINK_DELETED_EVENT_KEY =
      "ru.mail.jira.plugins.myteam.notification.issue.link.deleted";
  private final I18nResolver i18nResolver;
  private final MessageFormatter messageFormatter;

  @Autowired
  public JiraIssueLinkToChatMessageConverter(
      final @ComponentImport I18nResolver i18nResolver, final MessageFormatter messageFormatter) {
    this.i18nResolver = i18nResolver;
    this.messageFormatter = messageFormatter;
  }

  @Override
  public String convert(final IssueLinkEventToChatMessageData issueLinkEventToChatMessageData) {
    final String changesAuthor =
        messageFormatter.formatUserToVKTeamsSysProfile(
            issueLinkEventToChatMessageData.getEventCreator());
    final String sourceIssue =
        messageFormatter.createMarkdownIssueLink(
            issueLinkEventToChatMessageData.getIssueKeySource());
    final String destinationIssue =
        messageFormatter.createMarkdownIssueLink(
            issueLinkEventToChatMessageData.getIssueKeyDestination());

    final String i18keyForEventType =
        issueLinkEventToChatMessageData.isLinkCreated()
            ? I18N_ISSUE_LINK_CREATED_EVENT_KEY
            : I18N_ISSUE_LINK_DELETED_EVENT_KEY;

    return i18nResolver.getText(
        i18keyForEventType,
        changesAuthor,
        Utils.shieldText(issueLinkEventToChatMessageData.getLinkTypeName()),
        sourceIssue,
        destinationIssue);
  }
}
