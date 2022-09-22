/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import static ru.mail.jira.plugins.myteam.db.repository.FilterSubscriptionRepository.DATE_TIME_FORMATTER;

import java.time.ZoneId;
import javax.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;

@Getter
@Setter
@NoArgsConstructor
public class FilterSubscriptionDto {
  @Nullable @XmlElement private Integer id;

  @Nullable @XmlElement private Long filterId;

  @Nullable @XmlElement private UserDto user;

  @Nullable @XmlElement private String groupName;

  @Nullable @XmlElement private String cronExpression;

  @Nullable @XmlElement private String lastRun;

  @XmlElement private Boolean emailOnEmpty;

  public FilterSubscriptionDto(FilterSubscription filterSubscription) {
    this.id = filterSubscription.getID();
    this.filterId = filterSubscription.getFilterId();
    this.groupName = filterSubscription.getGroupName();
    this.cronExpression = filterSubscription.getCronExpression();
    if (filterSubscription.getLastRun() != null)
      this.lastRun =
          filterSubscription
              .getLastRun()
              .toInstant()
              .atZone(ZoneId.systemDefault())
              .toLocalDateTime()
              .format(DATE_TIME_FORMATTER);
    this.emailOnEmpty = filterSubscription.isEmailOnEmpty();
  }
}
