/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.dto;

import com.atlassian.jira.security.roles.ProjectRole;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@SuppressWarnings("NullAway")
@Getter
@Setter
@NoArgsConstructor
public class ProjectRoleDto {
  @NotNull @XmlElement private Long id;

  @NotNull @XmlElement private String name;

  public ProjectRoleDto(ProjectRole projectRole) {
    this.id = projectRole.getId();
    this.name = projectRole.getName();
  }
}
