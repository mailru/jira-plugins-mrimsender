/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller.dto;

import com.atlassian.crowd.embedded.api.Group;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("NullAway")
public class GroupDto {
  @NotNull @XmlElement private String name;

  public GroupDto(Group group) {
    this.name = group.getName();
  }
}
