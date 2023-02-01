/* (C)2022 */
package ru.mail.jira.plugins.myteam.service;

import static ru.mail.jira.plugins.myteam.bot.rulesengine.states.JqlSearchState.JQL_SEARCH_PAGE_SIZE;
import static ru.mail.jira.plugins.myteam.bot.rulesengine.states.JqlSearchState.JQL_SEARCH_PAGE_SIZE_MAX;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.atlassian.scheduler.config.Schedule;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.bot.rulesengine.rules.commands.issue.ViewIssueCommandRule;
import ru.mail.jira.plugins.myteam.commons.Const;
import ru.mail.jira.plugins.myteam.component.MessageFormatter;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionDto;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscriptionType;
import ru.mail.jira.plugins.myteam.db.model.RecipientsType;
import ru.mail.jira.plugins.myteam.db.repository.FilterSubscriptionRepository;
import ru.mail.jira.plugins.myteam.myteam.MyteamApiClient;

@Component
@Validated
public class FilterSubscriptionService {
  private static final JobRunnerKey JOB_RUNNER_KEY =
      JobRunnerKey.of(FilterSubscriptionService.class.getName());

  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final I18nResolver i18nResolver;
  private final GroupManager groupManager;
  private final SchedulerService schedulerService;
  private final SearchRequestService searchRequestService;
  private final UserManager userManager;
  private final FilterSubscriptionRepository filterSubscriptionRepository;
  private final IssueService issueService;
  private final MyteamApiClient myteamClient;
  private final UserChatService userChatService;

  public FilterSubscriptionService(
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport GroupManager groupManager,
      @ComponentImport SchedulerService schedulerService,
      @ComponentImport SearchRequestService searchRequestService,
      @ComponentImport UserManager userManager,
      FilterSubscriptionRepository filterSubscriptionRepository,
      IssueService issueService,
      MyteamApiClient myteamClient,
      UserChatService userChatService) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.i18nResolver = i18nResolver;
    this.issueService = issueService;
    this.groupManager = groupManager;
    this.searchRequestService = searchRequestService;
    this.userManager = userManager;
    this.schedulerService = schedulerService;
    this.filterSubscriptionRepository = filterSubscriptionRepository;
    this.myteamClient = myteamClient;
    this.userChatService = userChatService;
    schedulerService.registerJobRunner(
        JOB_RUNNER_KEY, new SendMyteamSubscriptionNotificationsJob());
  }

  public FilterSubscription createFilterSubscription(
      @Valid FilterSubscriptionDto filterSubscriptionDto) {
    FilterSubscription subscription = filterSubscriptionRepository.create(filterSubscriptionDto);
    createScheduleJob(subscription.getID(), subscription.getCronExpression());
    return subscription;
  }

  public FilterSubscription updateFilterSubscription(
      int filterSubscriptionId, @Valid FilterSubscriptionDto filterSubscriptionDto) {
    FilterSubscription subscription =
        filterSubscriptionRepository.update(filterSubscriptionId, filterSubscriptionDto);
    updateScheduleJob(subscription.getID(), subscription.getCronExpression());
    return subscription;
  }

  public void deleteFilterSubscription(int filterSubscriptionId) {
    filterSubscriptionRepository.deleteById(filterSubscriptionId);
    deleteScheduleJob(filterSubscriptionId);
  }

  public void runFilterSubscription(int subscriptionId) {
    FilterSubscription filterSubscription = filterSubscriptionRepository.get(subscriptionId);
    filterSubscriptionRepository.updateLastRun(
        subscriptionId, LocalDateTime.now(ZoneId.systemDefault()));
    sendMyteamNotifications(filterSubscription);
  }

  private JobConfig getJobConfig(int scheduleId, Schedule schedule) {
    return JobConfig.forJobRunnerKey(JOB_RUNNER_KEY)
        .withSchedule(schedule)
        .withParameters(Map.of(Const.SCHEDULE_ID, scheduleId));
  }

  public static JobId getJobId(final int subscriptionId) {
    return JobId.of(FilterSubscriptionService.class.getName() + ':' + subscriptionId);
  }

  private void createScheduleJob(int subscriptionId, @NotNull String cronExpression) {
    try {
      Schedule schedule = Schedule.forCronExpression(cronExpression);
      JobConfig config = getJobConfig(subscriptionId, schedule);
      schedulerService.scheduleJob(getJobId(subscriptionId), config);
    } catch (final SchedulerServiceException e) {
      throw new DataAccessException(e);
    }
  }

  private void updateScheduleJob(int subscriptionId, @NotNull String cronExpression) {
    try {
      Schedule schedule = Schedule.forCronExpression(cronExpression);
      deleteScheduleJob(subscriptionId);
      JobConfig config = getJobConfig(subscriptionId, schedule);
      schedulerService.scheduleJob(getJobId(subscriptionId), config);
    } catch (final SchedulerServiceException e) {
      throw new DataAccessException(e);
    }
  }

  private void deleteScheduleJob(int subscriptionId) {
    JobId jobId = getJobId(subscriptionId);
    if (schedulerService.getJobDetails(jobId) != null) schedulerService.unscheduleJob(jobId);
    else
      SentryClient.capture(
          String.format(
              "Unable to find a scheduled job for myteam subscription schedule: %d. Removing the schedule anyway.",
              subscriptionId));
  }

  private Query buildJqlQuery(
      SearchRequest searchRequest,
      FilterSubscriptionType subscriptionType,
      @Nullable Date lastRun) {
    Query searchRequestQuery = searchRequest.getQuery();
    if (lastRun == null || subscriptionType.equals(FilterSubscriptionType.ALL))
      return searchRequestQuery;

    JqlQueryBuilder jqlQueryBuilder = JqlQueryBuilder.newBuilder();
    jqlQueryBuilder.where().and().sub().addClause(searchRequestQuery.getWhereClause()).endsub();
    if (subscriptionType.equals(FilterSubscriptionType.CREATED)) {
      jqlQueryBuilder.where().and().sub().createdAfter(lastRun).endsub();
    } else if (subscriptionType.equals(FilterSubscriptionType.UPDATED)) {
      jqlQueryBuilder.where().and().sub().updatedAfter(lastRun).endsub();
    } else {
      jqlQueryBuilder.where().and().sub().createdAfter(lastRun).or().updatedAfter(lastRun).endsub();
    }
    jqlQueryBuilder
        .orderBy()
        .setSorts(
            JqlQueryBuilder.newOrderByBuilder()
                .setSorts(searchRequestQuery.getOrderByClause())
                .buildOrderBy());

    return jqlQueryBuilder.buildQuery();
  }

  private void sendMessage(
      String message,
      @Nullable ApplicationUser user,
      @Nullable String chatId,
      @Nullable Issue issue) {
    ApplicationUser currentUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      if (user != null) {
        jiraAuthenticationContext.setLoggedInUser(user);
        if (issue != null) {
          userChatService.sendMessageText(
              user.getEmailAddress(),
              message,
              ViewIssueCommandRule.getIssueButtons(
                  issue.getKey(), user, issueService.isUserWatching(issue, user)));
        } else {
          userChatService.sendMessageText(user.getEmailAddress(), message);
        }
      }
      if (chatId != null) userChatService.sendMessageText(chatId, message);
    } catch (Exception e) {
      SentryClient.capture(e);
    } finally {
      jiraAuthenticationContext.setLoggedInUser(currentUser);
    }
  }

  private void sendMessages(
      FilterSubscription subscription,
      @Nullable ApplicationUser subscriber,
      @Nullable String chatId) {
    ApplicationUser creator = userManager.getUserByKey(subscription.getUserKey());

    ApplicationUser jqlUser = subscriber != null ? subscriber : creator;
    JiraServiceContextImpl jiraServiceContext = new JiraServiceContextImpl(jqlUser);
    SearchRequest searchRequest =
        searchRequestService.getFilter(jiraServiceContext, subscription.getFilterId());
    MessageFormatter messageFormatter = userChatService.getMessageFormatter();

    if (jiraServiceContext.getErrorCollection().hasAnyErrors()) {
      sendMessage(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.error",
              String.join(" ", jiraServiceContext.getErrorCollection().getErrorMessages())),
          creator,
          null,
          null);
      return;
    }

    try {
      Query jqlQuery =
          buildJqlQuery(searchRequest, subscription.getType(), subscription.getLastRun());
      int jqlLimit =
          subscription.isSeparateIssues() && subscriber != null
              ? JQL_SEARCH_PAGE_SIZE_MAX
              : JQL_SEARCH_PAGE_SIZE;
      SearchResults<Issue> searchResults =
          issueService.searchByJqlQuery(jqlQuery, jqlUser, 0, jqlLimit);

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

      if (subscription.isSeparateIssues() && subscriber != null) {
        sendMessage(
            messageFormatter.formatIssueFilterSubscription(
                searchRequest.getName(), searchRequest.getId(), searchResults.getTotal()),
            subscriber,
            chatId,
            null);
        for (Issue issue : searchResults.getResults()) {
          sendMessage(
              messageFormatter.createIssueSummary(issue, subscriber), subscriber, chatId, issue);
        }
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
      sendMessage(
          i18nResolver.getText(
              "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.error",
              String.join(" ", e.getLocalizedMessage())),
          creator,
          null,
          null);
    }
  }

  private void sendMyteamNotifications(FilterSubscription subscription) {
    RecipientsType recipientsType = subscription.getRecipientsType();
    if (recipientsType.equals(RecipientsType.USER)) {
      for (String userKey : CommonUtils.split(subscription.getRecipients())) {
        ApplicationUser user = userManager.getUserByKey(userKey);
        if (user != null) {
          sendMessages(subscription, user, null);
        }
      }
    } else if (recipientsType.equals(RecipientsType.GROUP)) {
      for (String groupName : CommonUtils.split(subscription.getRecipients())) {
        Group group = groupManager.getGroup(groupName);
        if (group != null) {
          for (ApplicationUser user : groupManager.getUsersInGroup(group)) {
            if (user != null) {
              sendMessages(subscription, user, null);
            }
          }
        }
      }
    } else if (recipientsType.equals(RecipientsType.CHAT)) {
      for (String chat : CommonUtils.split(subscription.getRecipients())) {
        try {
          myteamClient.getChatInfo(chat);
        } catch (Exception e) {
          ApplicationUser creator = userManager.getUserByKey(subscription.getUserKey());

          sendMessage(
              i18nResolver.getText(
                  "ru.mail.jira.plugins.myteam.subscriptions.page.subscription.error.chat.notExist",
                  chat),
              creator,
              null,
              null);
        }
        sendMessages(subscription, null, chat);
      }
    }
  }

  class SendMyteamSubscriptionNotificationsJob implements JobRunner {
    @Nullable
    @Override
    public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
      final Map<String, Serializable> parameters = jobRunnerRequest.getJobConfig().getParameters();
      Integer subscriptionId = (Integer) parameters.get(Const.SCHEDULE_ID);
      if (subscriptionId == null) {
        return JobRunnerResponse.failed(
            String.format("No My Team subscription for id %d", subscriptionId));
      }

      try {
        FilterSubscription subscription = filterSubscriptionRepository.get(subscriptionId);
        filterSubscriptionRepository.updateLastRun(
            subscriptionId, LocalDateTime.now(ZoneId.systemDefault()));
        sendMyteamNotifications(subscription);
        return JobRunnerResponse.success();
      } catch (Exception e) {
        SentryClient.capture(e);
        return JobRunnerResponse.failed(
            String.format(
                "Fail send notification for My Team subscription with id %d. Error: %s",
                subscriptionId, e.getMessage()));
      }
    }
  }
}
