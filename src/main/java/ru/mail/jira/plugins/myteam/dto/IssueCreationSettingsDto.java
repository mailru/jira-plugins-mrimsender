/* (C)2022 */
package ru.mail.jira.plugins.myteam.dto;

import java.util.Arrays;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.*;
import ru.mail.jira.plugins.myteam.model.IssueCreationSettings;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@XmlRootElement
@SuppressWarnings("MissingSummary")
public class IssueCreationSettingsDto {

  public static String LABELS_DELIMITER = ";";
  @XmlElement private Integer id;
  @XmlElement private String chatId;
  @XmlElement private Boolean enabled;
  @XmlElement private String projectKey;
  @XmlElement private String issueTypeId;
  @XmlElement private String tag;
  @XmlElement private List<String> labels;

  public IssueCreationSettingsDto(IssueCreationSettings entity) {
    this.id = entity.getID();
    this.chatId = entity.getChatId();
    this.enabled = entity.isEnabled();
    this.tag = entity.getTag();
    this.projectKey = entity.getProjectKey();
    this.issueTypeId = entity.getIssueTypeId();
    this.labels =
        entity.getLabels() != null
            ? Arrays.asList(entity.getLabels().split(LABELS_DELIMITER))
            : null;
  }
}
