/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.*;
import ru.mail.jira.plugins.myteam.commons.IssueReporter;
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
  @XmlElement private IssueReporter reporter;
  @XmlElement private List<String> labels;
  @XmlElement private List<AdditionalIssueFieldDto> additionalFields;

  public IssueCreationSettingsDto(IssueCreationSettings entity) {
    this.id = entity.getID();
    this.chatId = entity.getChatId();
    this.enabled = entity.isEnabled();
    this.tag = entity.getTag();
    this.reporter = entity.getReporter();
    this.projectKey = entity.getProjectKey();
    this.issueTypeId = entity.getIssueTypeId();
    this.labels =
        entity.getLabels() != null
            ? Arrays.asList(entity.getLabels().split(LABELS_DELIMITER))
            : null;
    this.additionalFields =
        Arrays.stream(entity.getAdditionalFields())
            .map(AdditionalIssueFieldDto::new)
            .collect(Collectors.toList());
  }
}
