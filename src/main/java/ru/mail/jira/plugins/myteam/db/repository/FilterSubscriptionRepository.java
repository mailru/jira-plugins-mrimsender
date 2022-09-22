/* (C)2022 */
package ru.mail.jira.plugins.myteam.db.repository;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.dao.PagingAndSortingRepository;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionDto;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;

@Component
public class FilterSubscriptionRepository
    extends PagingAndSortingRepository<FilterSubscription, FilterSubscriptionDto> {
  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  private final UserManager userManager;

  public FilterSubscriptionRepository(ActiveObjects ao, @ComponentImport UserManager userManager) {
    super(ao);
    this.userManager = userManager;
  }

  @Override
  public FilterSubscriptionDto entityToDto(@NotNull FilterSubscription entity) {
    ApplicationUser user = userManager.getUserByKey(entity.getUserKey());
    FilterSubscriptionDto dto = new FilterSubscriptionDto(entity);
    dto.setUser(new UserDto(user));
    return dto;
  }

  @Override
  public void updateEntityFromDto(
      @NotNull FilterSubscriptionDto dto, @NotNull FilterSubscription entity) {
    entity.setFieldId(Objects.requireNonNull(dto.getFilterId()));
    if (dto.getUser() != null)
      entity.setUserKey(Objects.requireNonNull(dto.getUser().getUserKey()));
    entity.setGroupName(dto.getGroupName());
    entity.setCronExpression(Objects.requireNonNull(dto.getCronExpression()));
    if (dto.getLastRun() != null)
      entity.setLastRun(
          Date.from(
              LocalDateTime.parse(dto.getLastRun(), DATE_TIME_FORMATTER)
                  .atZone(ZoneId.systemDefault())
                  .toInstant()));
    entity.setEmailOnEmpty(Objects.requireNonNull(dto.getEmailOnEmpty()));
  }

  @Override
  public @Nullable String mapDbField(@Nullable String s) {
    return null;
  }
}
