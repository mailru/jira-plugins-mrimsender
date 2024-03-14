/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.subscription;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.service.CommonButtonsService;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;
import ru.mail.jira.plugins.myteam.db.model.RecipientsType;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.UserChatService;

@Component
public class FilterSubscriptionByGroupSender extends AbstractFilterSubscriptionSender {

  @Autowired
  public FilterSubscriptionByGroupSender(
      @ComponentImport final JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport final I18nResolver i18nResolver,
      @ComponentImport final GroupManager groupManager,
      @ComponentImport final UserManager userManager,
      @ComponentImport final SearchRequestService searchRequestService,
      final IssueService issueService,
      final UserChatService userChatService,
      final CommonButtonsService commonButtonsService) {
    super(
        jiraAuthenticationContext,
        i18nResolver,
        groupManager,
        userManager,
        searchRequestService,
        issueService,
        userChatService,
        commonButtonsService);
  }

  @Override
  public void sendMyteamNotifications(final FilterSubscription subscription) {
    for (final String groupName : CommonUtils.split(subscription.getRecipients())) {
      final Group group = groupManager.getGroup(groupName);
      if (group != null) {
        for (final ApplicationUser user : groupManager.getUsersInGroup(group)) {
          if (user != null) {
            super.sendMessages(subscription, user, null);
          }
        }
      }
    }
  }

  @Override
  public RecipientsType getRecipientType() {
    return RecipientsType.GROUP;
  }
}
