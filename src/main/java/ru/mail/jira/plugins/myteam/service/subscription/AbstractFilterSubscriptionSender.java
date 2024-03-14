/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.subscription;

import static ru.mail.jira.plugins.myteam.bot.rulesengine.states.JqlSearchState.JQL_SEARCH_PAGE_SIZE;
import static ru.mail.jira.plugins.myteam.bot.rulesengine.states.JqlSearchState.JQL_SEARCH_PAGE_SIZE_MAX;

import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.query.Query;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.Date;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.service.CommonButtonsService;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscriptionType;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.UserChatService;

public abstract class AbstractFilterSubscriptionSender implements FilterSubscriptionSender {
  protected final JiraAuthenticationContext jiraAuthenticationContext;
  protected final I18nResolver i18nResolver;
  protected final GroupManager groupManager;
  protected final UserManager userManager;
  protected final SearchRequestService searchRequestService;
  protected final IssueService issueService;
  protected final UserChatService userChatService;
  protected final CommonButtonsService commonButtonsService;

  public AbstractFilterSubscriptionSender(
      final JiraAuthenticationContext jiraAuthenticationContext,
      final I18nResolver i18nResolver,
      final GroupManager groupManager,
      final UserManager userManager,
      final SearchRequestService searchRequestService,
      final IssueService issueService,
      final UserChatService userChatService,
      final CommonButtonsService commonButtonsService) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.i18nResolver = i18nResolver;
    this.groupManager = groupManager;
    this.userManager = userManager;
    this.searchRequestService = searchRequestService;
    this.issueService = issueService;
    this.userChatService = userChatService;
    this.commonButtonsService = commonButtonsService;
  }

  protected void sendMessages(
      final FilterSubscription subscription,
      @Nullable final ApplicationUser subscriber,
      @Nullable final String chatId) {
    final ApplicationUser currentUser = jiraAuthenticationContext.getLoggedInUser();
    final ApplicationUser creator = userManager.getUserByKey(subscription.getUserKey());
    final ApplicationUser jqlUser = subscriber != null ? subscriber : creator;

    try {
      jiraAuthenticationContext.setLoggedInUser(jqlUser);
      final JiraServiceContextImpl jiraServiceContext = new JiraServiceContextImpl(jqlUser);
      final SearchRequest searchRequest =
          searchRequestService.getFilter(jiraServiceContext, subscription.getFilterId());
      final MessageFormatter messageFormatter = userChatService.getMessageFormatter();

      if (jiraServiceContext.getErrorCollection().hasAnyErrors()) {
        jiraAuthenticationContext.setLoggedInUser(creator);
        sendMessage(
            i18nResolver.getText(
                "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.error",
                String.join(" ", jiraServiceContext.getErrorCollection().getErrorMessages())),
            creator,
            null,
            null);
        jiraAuthenticationContext.setLoggedInUser(currentUser);
        return;
      }

      final SearchResults<Issue> searchResults =
          searchIssuesToSend(subscription, subscriber, searchRequest, jqlUser);

      if (searchResults.getTotal() == 0) {
        if (subscription.isEmailOnEmpty()) {
          sendMessage(
              messageFormatter.formatEmptyFilterSubscription(
                  searchRequest.getName(), searchRequest.getId()),
              subscriber,
              chatId,
              null);
        }
        return;
      }

      if (subscription.isSeparateIssues()) {
        sendSeparatedIssues(
            subscriber, chatId, messageFormatter, searchRequest, searchResults, jqlUser);
      } else {
        sendMessage(
            messageFormatter.formatListFilterSubscription(
                searchRequest.getName(),
                searchRequest.getId(),
                searchRequest.getQuery().getQueryString(),
                searchResults),
            subscriber,
            chatId,
            null);
      }
    } catch (Exception e) {
      jiraAuthenticationContext.setLoggedInUser(creator);
      sendMessage(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.error",
              String.join(" ", e.getLocalizedMessage())),
          creator,
          null,
          null);
    } finally {
      jiraAuthenticationContext.setLoggedInUser(currentUser);
    }
  }

  private SearchResults<Issue> searchIssuesToSend(
      final FilterSubscription subscription,
      @Nullable final ApplicationUser subscriber,
      final SearchRequest searchRequest,
      final ApplicationUser jqlUser)
      throws SearchException, ParseException {
    final Query jqlQuery =
        buildJqlQuery(searchRequest, subscription.getType(), subscription.getLastRun());
    final int jqlLimit =
        subscription.isSeparateIssues() && subscriber != null
            ? JQL_SEARCH_PAGE_SIZE_MAX
            : JQL_SEARCH_PAGE_SIZE;
    return issueService.searchByJqlQuery(jqlQuery, jqlUser, 0, jqlLimit);
  }

  private Query buildJqlQuery(
      final SearchRequest searchRequest,
      final FilterSubscriptionType subscriptionType,
      @Nullable final Date lastRun) {
    final Query searchRequestQuery = searchRequest.getQuery();
    if (lastRun == null || subscriptionType.equals(FilterSubscriptionType.ALL)) {
      return searchRequestQuery;
    }

    final JqlQueryBuilder jqlQueryBuilder = JqlQueryBuilder.newBuilder();
    jqlQueryBuilder.where().and().sub().addClause(searchRequestQuery.getWhereClause()).endsub();

    if (subscriptionType.equals(FilterSubscriptionType.CREATED)) {
      jqlQueryBuilder.where().and().sub().createdAfter(lastRun).endsub();
    } else if (subscriptionType.equals(FilterSubscriptionType.UPDATED)) {
      jqlQueryBuilder.where().and().sub().updatedAfter(lastRun).endsub();
    } else {
      jqlQueryBuilder.where().and().sub().createdAfter(lastRun).or().updatedAfter(lastRun).endsub();
    }

    return jqlQueryBuilder
        .orderBy()
        .setSorts(
            JqlQueryBuilder.newOrderByBuilder()
                .setSorts(searchRequestQuery.getOrderByClause())
                .buildOrderBy())
        .buildQuery();
  }

  protected void sendMessage(
      final String message,
      @Nullable final ApplicationUser user,
      @Nullable final String chatId,
      @Nullable final Issue issue) {
    try {
      if (user != null) {
        if (issue != null) {
          userChatService.sendMessageText(
              user.getEmailAddress(),
              message,
              commonButtonsService.getIssueButtons(
                  issue.getKey(), user, issueService.isUserWatching(issue, user)));
        } else {
          userChatService.sendMessageText(user.getEmailAddress(), message);
        }
      }
      if (chatId != null) {
        userChatService.sendMessageText(chatId, message);
      }
    } catch (Exception e) {
      SentryClient.capture(
          e,
          Map.of(
              "user",
              user != null ? user.getKey() : StringUtils.EMPTY,
              "chatId",
              String.valueOf(chatId),
              "issueKey",
              issue != null ? issue.getKey() : StringUtils.EMPTY));
    }
  }

  private void sendSeparatedIssues(
      @Nullable final ApplicationUser subscriber,
      @Nullable final String chatId,
      final MessageFormatter messageFormatter,
      final SearchRequest searchRequest,
      final SearchResults<Issue> searchResults,
      final ApplicationUser jqlUser) {
    sendMessage(
        messageFormatter.formatIssueFilterSubscription(
            searchRequest.getName(), searchRequest.getId(), searchResults.getTotal()),
        subscriber,
        chatId,
        null);
    for (final Issue issue : searchResults.getResults()) {
      sendMessage(messageFormatter.createIssueSummary(issue, jqlUser), subscriber, chatId, issue);
    }
  }
}
