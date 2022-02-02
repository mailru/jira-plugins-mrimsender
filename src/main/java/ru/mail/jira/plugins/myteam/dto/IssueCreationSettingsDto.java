package ru.mail.jira.plugins.myteam.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.mail.jira.plugins.myteam.model.IssueCreationSettingsEntity;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class IssueCreationSettingsDto {

  public static String LABELS_DELIMITER = ";";

  private int id;

  private String chatId;

  private boolean enabled;

  private String projectKey;

  private String issueTypeId;

  private String tag;

  private List<String> labels;

  public IssueCreationSettingsDto(IssueCreationSettingsEntity entity) {
    this.id = entity.getID();
    this.chatId = entity.getChatId();
    this.enabled = entity.isEnabled();
    this.tag = entity.getTag();
    this.projectKey = entity.getProjectKey();
    this.issueTypeId = entity.getIssueTypeId();
    this.labels = Arrays.asList(entity.getLabels().split(LABELS_DELIMITER));
  }

}
