/* (C)2022 */
package ru.mail.jira.plugins.myteam.service;

import javax.validation.Valid;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionDto;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;
import ru.mail.jira.plugins.myteam.db.repository.FilterSubscriptionRepository;

@Component
@Validated
@SuppressWarnings("NullAway")
public class FilterSubscriptionService {
  private final FilterSubscriptionRepository filterSubscriptionRepository;

  public FilterSubscriptionService(FilterSubscriptionRepository filterSubscriptionRepository) {
    this.filterSubscriptionRepository = filterSubscriptionRepository;
  }

  public FilterSubscription createFilterSubscription(
      @Valid FilterSubscriptionDto filterSubscriptionDto) {
    return filterSubscriptionRepository.create(filterSubscriptionDto);
  }

  public FilterSubscription updateFilterSubscription(
      int filterSubscriptionId, @Valid FilterSubscriptionDto filterSubscriptionDto) {
    return filterSubscriptionRepository.update(filterSubscriptionId, filterSubscriptionDto);
  }
}
