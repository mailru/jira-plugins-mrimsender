/* (C)2023 */
package ru.mail.jira.plugins.myteam.controller.dto;

import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.db.model.Reminder;

@SuppressWarnings({"MissingSummary", "NullAway", "CanIgnoreReturnValueSuggester"})
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@XmlRootElement
public class ReminderDto {
  @XmlElement int id;
  @XmlElement @Nullable String issueKey;
  @XmlElement @Nullable private String userEmail;
  @XmlElement @Nullable private Date date;
  @XmlElement @Nullable private String description;

  public ReminderDto(Reminder entity) {
    this.id = entity.getID();
    this.issueKey = entity.getIssueKey();
    this.date = entity.getDate();
    this.userEmail = entity.getUserEmail();
    this.description = entity.getDescription();
  }
}
