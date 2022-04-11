/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.*;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.commons.IssueReporter;
import ru.mail.jira.plugins.myteam.model.IssueCreationSettings;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("MissingSummary")
public class IssueCreationSettingsDto {

  public static String LABELS_DELIMITER = ";";
  @XmlElement private Integer id;
  @XmlElement private String chatId;
  @XmlElement private Boolean enabled;
  @XmlElement private String projectKey;
  @XmlElement private String issueTypeId;
  @XmlElement private String tag;
  @XmlElement private String creationSuccessTemplate;
  @XmlElement private String issueSummaryTemplate;
  @Nullable @XmlElement private String chatLink;
  @Nullable @XmlElement private String chatTitle;
  @Nullable @XmlElement private String issueTypeName;
  @XmlElement private Boolean canEdit;
  @XmlElement private IssueReporter reporter;
  @XmlElement private Boolean addReporterInWatchers;
  @XmlElement private List<String> labels;
  @XmlElement private List<AdditionalIssueFieldDto> additionalFields;

  public IssueCreationSettingsDto(IssueCreationSettings entity) {
    this.id = entity.getID();
    this.chatId = entity.getChatId();
    this.enabled = entity.isEnabled();
    this.tag = entity.getTag();
    this.reporter = entity.getReporter();
    this.addReporterInWatchers = entity.isAddReporterInWatchers();
    this.projectKey = entity.getProjectKey();
    this.issueTypeId = entity.getIssueTypeId();
    this.creationSuccessTemplate = entity.getCreationSuccessTemplate();
    this.issueSummaryTemplate = entity.getIssueSummaryTemplate();
    this.labels =
        entity.getLabels() != null
            ? Arrays.asList(entity.getLabels().split(LABELS_DELIMITER))
            : null;
    this.additionalFields =
        Arrays.stream(entity.getAdditionalFields())
            .map(AdditionalIssueFieldDto::new)
            .collect(Collectors.toList());
  }

  public IssueCreationSettingsDto(IssueCreationSettings entity, @Nullable String chatLink) {
    this.canEdit = true;
    this.id = entity.getID();
    this.chatId = entity.getChatId();
    this.enabled = entity.isEnabled();
    this.tag = entity.getTag();
    this.reporter = entity.getReporter();
    this.addReporterInWatchers = entity.isAddReporterInWatchers();
    this.projectKey = entity.getProjectKey();
    this.issueTypeId = entity.getIssueTypeId();
    this.creationSuccessTemplate = entity.getCreationSuccessTemplate();
    this.issueSummaryTemplate = entity.getIssueSummaryTemplate();
    this.labels =
        entity.getLabels() != null
            ? Arrays.asList(entity.getLabels().split(LABELS_DELIMITER))
            : null;
    this.additionalFields =
        Arrays.stream(entity.getAdditionalFields())
            .map(AdditionalIssueFieldDto::new)
            .collect(Collectors.toList());
    this.chatLink = chatLink;
  }
}
