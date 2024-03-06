/* (C)2022 */
package ru.mail.jira.plugins.myteam.service;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.scheduler.config.JobRunnerKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import ru.mail.jira.plugins.commons.SentryClient;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionDto;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;
import ru.mail.jira.plugins.myteam.db.model.RecipientsType;
import ru.mail.jira.plugins.myteam.db.repository.FilterSubscriptionRepository;
import ru.mail.jira.plugins.myteam.service.subscription.FilterSubscriptionResolver;
import ru.mail.jira.plugins.myteam.service.subscription.FilterSubscriptionSchedulerService;

@Component
@Validated
public class FilterSubscriptionService {
  public static final JobRunnerKey JOB_RUNNER_KEY =
      JobRunnerKey.of(FilterSubscriptionService.class.getName());

  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final FilterSubscriptionRepository filterSubscriptionRepository;
  private final FilterSubscriptionSchedulerService filterSubscriptionSchedulerService;
  private final FilterSubscriptionResolver filterSubscriptionResolver;
  private final PermissionHelper permissionHelper;

  @Autowired
  public FilterSubscriptionService(
      @ComponentImport final JiraAuthenticationContext jiraAuthenticationContext,
      final FilterSubscriptionRepository filterSubscriptionRepository,
      final FilterSubscriptionSchedulerService filterSubscriptionSchedulerService,
      final FilterSubscriptionResolver filterSubscriptionResolver,
      final PermissionHelper permissionHelper) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.filterSubscriptionRepository = filterSubscriptionRepository;
    this.filterSubscriptionSchedulerService = filterSubscriptionSchedulerService;
    this.filterSubscriptionResolver = filterSubscriptionResolver;
    this.permissionHelper = permissionHelper;
  }

  public FilterSubscription createFilterSubscription(
      @Valid final FilterSubscriptionDto filterSubscriptionDto) {
    checkLoggedInUser();

    final FilterSubscription subscription =
        filterSubscriptionRepository.create(filterSubscriptionDto);
    filterSubscriptionSchedulerService.createScheduleJob(
        subscription.getID(), subscription.getCronExpression());
    return subscription;
  }

  public FilterSubscription updateFilterSubscription(
      int filterSubscriptionId, @Valid final FilterSubscriptionDto filterSubscriptionDto) {
    checkCurrentUserAndSubscriptionPermission(filterSubscriptionId);

    final FilterSubscription updatedSubscription =
        filterSubscriptionRepository.update(filterSubscriptionId, filterSubscriptionDto);
    filterSubscriptionSchedulerService.updateScheduleJob(
        updatedSubscription.getID(), updatedSubscription.getCronExpression());
    return updatedSubscription;
  }

  public void deleteFilterSubscription(final int filterSubscriptionId) {
    checkCurrentUserAndSubscriptionPermission(filterSubscriptionId);

    filterSubscriptionRepository.deleteById(filterSubscriptionId);
    filterSubscriptionSchedulerService.deleteScheduleJob(filterSubscriptionId);
  }

  public void runFilterSubscriptionIfUserHasPermission(final int filterSubscriptionId) {
    checkCurrentUserAndSubscriptionPermission(filterSubscriptionId);
    runFilterSubscription(filterSubscriptionId);
  }

  public void runFilterSubscription(final int filterSubscriptionId) {
    final FilterSubscription filterSubscription =
        filterSubscriptionRepository.get(filterSubscriptionId);
    filterSubscriptionRepository.updateLastRun(
        filterSubscriptionId, LocalDateTime.now(ZoneId.systemDefault()));
    filterSubscriptionResolver
        .resolve(filterSubscription.getRecipientsType())
        .ifPresentOrElse(
            filterSubscriptionSender ->
                filterSubscriptionSender.sendMyteamNotifications(filterSubscription),
            () ->
                SentryClient.capture(
                    String.format(
                        "FilterSubscriptionSender was not resolve by filter subscription [id: %s, recipient type: %s]",
                        filterSubscription.getID(), filterSubscription.getRecipientsType())));
  }

  public List<FilterSubscriptionDto> getSubscriptions(
      final List<String> subscribers,
      final Long filterId,
      final RecipientsType recipientsType,
      final List<String> recipients) {
    final ApplicationUser loggedInUser = checkLoggedInUser();

    final List<String> subscriberUsers;
    if (permissionHelper.isJiraAdmin(loggedInUser)) {
      subscriberUsers = subscribers;
    } else {
      subscriberUsers = Collections.singletonList(loggedInUser.getKey());
    }

    return Arrays.stream(
            filterSubscriptionRepository.getSubscriptions(
                subscriberUsers, filterId, recipientsType, recipients))
        .map(filterSubscriptionRepository::entityToDto)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private void checkCurrentUserAndSubscriptionPermission(final int filterSubscriptionId) {
    final ApplicationUser loggedInUser = checkLoggedInUser();
    final FilterSubscription subscription = filterSubscriptionRepository.get(filterSubscriptionId);
    permissionHelper.checkSubscriptionPermission(loggedInUser, subscription);
  }

  @NotNull
  private ApplicationUser checkLoggedInUser() {
    final ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }
    return loggedInUser;
  }
}
