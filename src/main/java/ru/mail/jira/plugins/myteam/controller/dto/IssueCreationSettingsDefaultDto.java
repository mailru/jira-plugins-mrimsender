/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import static ru.mail.jira.plugins.myteam.commons.Const.DEFAULT_ISSUE_CREATION_SUCCESS_TEMPLATE;
import static ru.mail.jira.plugins.myteam.commons.Const.DEFAULT_ISSUE_SUMMARY_TEMPLATE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.*;
import ru.mail.jira.plugins.myteam.model.IssueCreationSettingsDefault;

@Getter
@Setter
@AllArgsConstructor
@Builder
@ToString
@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("MissingSummary")
public class IssueCreationSettingsDefaultDto {
  @XmlElement private String creationSuccessTemplate;
  @XmlElement private String issueSummaryTemplate;

  public IssueCreationSettingsDefaultDto(IssueCreationSettingsDefault entity) {
    this.creationSuccessTemplate = entity.getCreationSuccessTemplate();
    this.creationSuccessTemplate = entity.getIssueSummaryTemplate();
  }

  public IssueCreationSettingsDefaultDto() {
    this.creationSuccessTemplate = DEFAULT_ISSUE_CREATION_SUCCESS_TEMPLATE;
    this.issueSummaryTemplate = DEFAULT_ISSUE_SUMMARY_TEMPLATE;
  }
}
