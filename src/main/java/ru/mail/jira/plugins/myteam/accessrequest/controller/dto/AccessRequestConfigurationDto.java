/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.dto;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation.GroupsUserCountValidation;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation.OneNotFalseValidation;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation.OneNotNullValidation;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation.ProjectRolesUserCountValidation;

@SuppressWarnings("NullAway")
@Getter
@Setter
@NoArgsConstructor
@OneNotNullValidation(
    fields = {"users", "groups", "projectRoles", "userFields"},
    errorField = "participants")
@OneNotFalseValidation(
    fields = {"sendEmail", "sendMessage"},
    errorField = "notifications")
@ProjectRolesUserCountValidation
public class AccessRequestConfigurationDto {
  @Nullable @XmlElement private Integer id;

  @NotNull @XmlElement private String projectKey;

  @Nullable @XmlElement private List<UserDto> users;

  @GroupsUserCountValidation @Nullable @XmlElement private List<String> groups;

  @Nullable @XmlElement private List<ProjectRoleDto> projectRoles;

  @Nullable @XmlElement private List<UserFieldDto> userFields;

  @XmlElement private boolean sendEmail;

  @XmlElement private boolean sendMessage;
}
