/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.dto;

import com.atlassian.jira.issue.fields.CustomField;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("NullAway")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserFieldDto {
  @NotNull @XmlElement private String id;

  @NotNull @XmlElement private String name;

  public UserFieldDto(CustomField customField) {
    this.id = customField.getId();
    this.name = customField.getName();
  }
}
