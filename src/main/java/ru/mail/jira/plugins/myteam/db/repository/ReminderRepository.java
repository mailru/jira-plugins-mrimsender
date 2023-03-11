/* (C)2023 */
package ru.mail.jira.plugins.myteam.db.repository;

import com.atlassian.activeobjects.external.ActiveObjects;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import net.java.ao.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.dao.PagingAndSortingRepository;
import ru.mail.jira.plugins.myteam.controller.dto.ReminderDto;
import ru.mail.jira.plugins.myteam.db.model.Reminder;

@Component
public class ReminderRepository extends PagingAndSortingRepository<Reminder, ReminderDto> {

  public ReminderRepository(ActiveObjects ao) {
    super(ao);
  }

  @Override
  public ReminderDto entityToDto(@NotNull Reminder entity) {
    return new ReminderDto(entity);
  }

  @Override
  public void updateEntityFromDto(@NotNull ReminderDto dto, @NotNull Reminder entity) {
    entity.setIssueKey(dto.getIssueKey());
    entity.setDate(dto.getDate());
    entity.setDescription(dto.getDescription());
    entity.setVKteamsUserId(dto.getVKteamsUserId());
  }

  @Override
  public @Nullable String mapDbField(@Nullable String s) {
    return null;
  }

  public Reminder[] getRemindersBeforeDate(LocalDateTime date) {
    Query query =
        Query.select()
            .where("date <= ?", Date.from(date.atZone(ZoneId.systemDefault()).toInstant()));
    return ao.find(Reminder.class, query);
  }
}
