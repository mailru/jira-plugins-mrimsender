/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import javax.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.mail.jira.plugins.myteam.db.model.AdditionalIssueField;

@SuppressWarnings("NullAway")
@Getter
@Setter
@NoArgsConstructor
public class AdditionalIssueFieldDto {
  @XmlElement private String field;

  @XmlElement private String value;

  public AdditionalIssueFieldDto(AdditionalIssueField additionalIssueField) {
    this.field = additionalIssueField.getFieldId();
    this.value = additionalIssueField.getValue();
  }
}
