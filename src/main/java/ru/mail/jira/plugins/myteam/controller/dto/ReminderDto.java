/* (C)2023 */
package ru.mail.jira.plugins.myteam.controller.dto;

import java.util.Date;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.db.model.Reminder;

@Getter
@Setter
@NoArgsConstructor
public class ReminderDto {
  int id;
  @Nullable String issueKey;
  @Nullable private String VKteamsUserId;
  @Nullable private Date date;
  @Nullable private String description;

  public ReminderDto(Reminder entity) {
    this.id = entity.getID();
    this.issueKey = entity.getIssueKey();
    this.date = entity.getDate();
    this.VKteamsUserId = entity.getVKteamsUserId();
    this.description = entity.getDescription();
  }
}
