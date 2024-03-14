/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.subscription;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.db.model.RecipientsType;

@Component
public class FilterSubscriptionResolver {

  private final Map<RecipientsType, FilterSubscriptionSender> filterSenderByTypeMap;

  @Autowired
  public FilterSubscriptionResolver(
      final List<FilterSubscriptionSender> filterSubscriptionSenders) {
    this.filterSenderByTypeMap =
        filterSubscriptionSenders.stream()
            .collect(
                Collectors.toMap(FilterSubscriptionSender::getRecipientType, Function.identity()));
  }

  public Optional<FilterSubscriptionSender> resolve(final RecipientsType recipientsType) {
    return Optional.ofNullable(filterSenderByTypeMap.get(recipientsType));
  }
}
