/* (C)2023 */
package ru.mail.jira.plugins.myteam.accessrequest.controller.validation;

import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestConfigurationDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.ProjectRoleDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.validation.annotation.ProjectRolesUserCountValidation;
import ru.mail.jira.plugins.myteam.controller.validation.provider.ContextProvider;

public class ProjectRolesProjectRolesValidator
    implements ConstraintValidator<ProjectRolesUserCountValidation, AccessRequestConfigurationDto> {
  public static final int MAX_USER_COUNT = 50;

  @Nullable private I18nHelper i18nHelper;
  @Nullable private ProjectManager projectManager;
  @Nullable private ProjectRoleManager projectRoleManager;

  @Override
  public void initialize(final ProjectRolesUserCountValidation constraint) {
    this.i18nHelper = (I18nHelper) ContextProvider.getBean(I18nHelper.class);
    this.projectManager = (ProjectManager) ContextProvider.getBean(ProjectManager.class);
    this.projectRoleManager =
        (ProjectRoleManager) ContextProvider.getBean(ProjectRoleManager.class);
  }

  @Override
  public boolean isValid(
      AccessRequestConfigurationDto accessRequestConfigurationDto,
      ConstraintValidatorContext constraintValidatorContext) {
    boolean isValid = true;
    if (i18nHelper != null && projectRoleManager != null && projectManager != null) {
      List<ProjectRoleDto> projectRoleDtos = accessRequestConfigurationDto.getProjectRoles();
      if (projectRoleDtos != null && !projectRoleDtos.isEmpty()) {
        Project project =
            projectManager.getProjectByCurrentKey(accessRequestConfigurationDto.getProjectKey());
        if (project == null) {
          isValid = false;
          constraintValidatorContext.disableDefaultConstraintViolation();
          constraintValidatorContext
              .buildConstraintViolationWithTemplate(
                  i18nHelper.getText(
                      "ru.mail.jira.plugins.myteam.accessRequest.configuration.page.error.validation.projectRoles.project"))
              .addPropertyNode("projectRoles")
              .addConstraintViolation();
        } else {
          for (ProjectRoleDto projectRoleDto : projectRoleDtos) {
            ProjectRole projectRole = projectRoleManager.getProjectRole(projectRoleDto.getId());
            if (projectRole != null) {
              Set<ApplicationUser> roleUsers =
                  projectRoleManager
                      .getProjectRoleActors(projectRole, project)
                      .getApplicationUsers();
              if (roleUsers != null && roleUsers.size() > MAX_USER_COUNT) {
                isValid = false;
                constraintValidatorContext.disableDefaultConstraintViolation();
                constraintValidatorContext
                    .buildConstraintViolationWithTemplate(
                        i18nHelper.getText(
                            "ru.mail.jira.plugins.myteam.accessRequest.configuration.page.error.validation.projectRoles.users",
                            String.valueOf(MAX_USER_COUNT),
                            projectRole.getName()))
                    .addPropertyNode("projectRoles")
                    .addConstraintViolation();
                break;
              }
            }
          }
        }
      }
    }
    return isValid;
  }
}
