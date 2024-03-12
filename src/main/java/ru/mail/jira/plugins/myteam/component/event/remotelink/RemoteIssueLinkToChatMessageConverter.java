/* (C)2024 */
package ru.mail.jira.plugins.myteam.component.event.remotelink;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.component.event.JiraEventToChatMessageConverter;

@Component
public class RemoteIssueLinkToChatMessageConverter
    implements JiraEventToChatMessageConverter<RemoteIssueLinkRecipientsData> {
  private static final String I18N_CREATED_REMOTE_ISSUE_LINK_KEY =
      "ru.mail.jira.plugins.myteam.notification.remote.issue.link.created";
  private final I18nResolver i18nResolver;
  private final MessageFormatter messageFormatter;

  @Autowired
  public RemoteIssueLinkToChatMessageConverter(
      @ComponentImport final I18nResolver i18nResolver, final MessageFormatter messageFormatter) {
    this.i18nResolver = i18nResolver;
    this.messageFormatter = messageFormatter;
  }

  @Override
  public String convert(final RemoteIssueLinkRecipientsData remoteIssueLinkRecipientsData) {
    final String markdownIssueLink =
        messageFormatter.createMarkdownIssueLink(remoteIssueLinkRecipientsData.getIssueKey());
    final String linkCreator =
        messageFormatter.formatUserToVKTeamsSysProfile(
            remoteIssueLinkRecipientsData.getLinkCreator());

    if (StringUtils.isNotBlank(remoteIssueLinkRecipientsData.getLinkTitle())) {
      return i18nResolver.getText(
          I18N_CREATED_REMOTE_ISSUE_LINK_KEY,
          linkCreator,
          messageFormatter.markdownTextLink(
              remoteIssueLinkRecipientsData.getLinkTitle(),
              remoteIssueLinkRecipientsData.getLinkUrl()),
          markdownIssueLink);
    } else {
      return i18nResolver.getText(
          I18N_CREATED_REMOTE_ISSUE_LINK_KEY,
          linkCreator,
          Utils.shieldText(remoteIssueLinkRecipientsData.getLinkUrl()),
          markdownIssueLink);
    }
  }
}
