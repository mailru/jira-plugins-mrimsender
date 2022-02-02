package ru.mail.jira.plugins.myteam.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.mail.jira.plugins.myteam.model.IssueCreationSettingsEntity;

@Getter
@Setter
@NoArgsConstructor
public class IssueCreationSettingsDto {
  private int id;

  private String chatId;

  private boolean enabled;

  private String projectKey;

  private String issueTypeId;

  public IssueCreationSettingsDto(IssueCreationSettingsEntity entity) {
    this.id = entity.getID();
    this.chatId = entity.getChatId();
    this.enabled = entity.isEnabled();
    this.projectKey = entity.getProjectKey();
    this.issueTypeId = entity.getIssueTypeId();
  }

}
