/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.model;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Objects;
import java.util.stream.Collectors;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.CommonUtils;
import ru.mail.jira.plugins.commons.dao.PagingAndSortingRepository;
import ru.mail.jira.plugins.commons.dto.jira.UserDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestConfigurationDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.DtoUtils;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.ProjectRoleDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.UserFieldDto;

@Component
@SuppressWarnings("NullAway")
public class AccessRequestConfigurationRepository
    extends PagingAndSortingRepository<AccessRequestConfiguration, AccessRequestConfigurationDto> {
  public static final String REPORTER = "reporter";
  public static final String ASSIGNEE = "assignee";
  public static final String WATCHERS = "watchers";

  private final CustomFieldManager customFieldManager;
  private final I18nHelper i18nHelper;
  private final ProjectManager projectManager;
  private final ProjectRoleManager projectRoleManager;
  private final UserManager userManager;
  private final DtoUtils dtoUtils;

  public AccessRequestConfigurationRepository(
      ActiveObjects ao,
      @ComponentImport CustomFieldManager customFieldManager,
      @ComponentImport I18nHelper i18nHelper,
      @ComponentImport ProjectManager projectManager,
      @ComponentImport ProjectRoleManager projectRoleManager,
      @ComponentImport UserManager userManager,
      DtoUtils dtoUtils) {
    super(ao);
    this.dtoUtils = dtoUtils;
    this.customFieldManager = customFieldManager;
    this.i18nHelper = i18nHelper;
    this.projectManager = projectManager;
    this.projectRoleManager = projectRoleManager;
    this.userManager = userManager;
  }

  @Override
  public AccessRequestConfigurationDto entityToDto(@NotNull AccessRequestConfiguration entity) {
    AccessRequestConfigurationDto dto = new AccessRequestConfigurationDto();
    dto.setId(entity.getID());
    dto.setProjectKey(
        Objects.requireNonNull(
            Objects.requireNonNull(projectManager.getProjectObj(entity.getProjectId())).getKey()));
    if (StringUtils.isNotBlank(entity.getUserKeys())) {
      dto.setUsers(
          CommonUtils.split(entity.getUserKeys()).stream()
              .map(key -> dtoUtils.buildUserDto(userManager.getUserByKey(key)))
              .collect(Collectors.toList()));
    }
    if (StringUtils.isNotBlank(entity.getGroups())) {
      dto.setGroups(CommonUtils.split(entity.getGroups()));
    }
    if (StringUtils.isNotBlank(entity.getProjectRoleIds())) {
      dto.setProjectRoles(
          CommonUtils.split(entity.getProjectRoleIds()).stream()
              .map(id -> new ProjectRoleDto(projectRoleManager.getProjectRole(Long.parseLong(id))))
              .collect(Collectors.toList()));
    }
    if (StringUtils.isNotBlank(entity.getUserFieldIds())) {
      dto.setUserFields(
          CommonUtils.split(entity.getUserFieldIds()).stream()
              .map(this::buildUserFieldDto)
              .collect(Collectors.toList()));
    }
    if (StringUtils.isNotBlank(entity.getVotersKeys())) {
      dto.setVotersFields(
          CommonUtils.split(entity.getVotersKeys()).stream()
              .map(key -> dtoUtils.buildUserDto(userManager.getUserByKey(key)))
              .collect(Collectors.toList()));
    }
    dto.setSendEmail(entity.isSendEmail());
    dto.setSendMessage(entity.isSendMessage());
    return dto;
  }

  @Override
  public void updateEntityFromDto(
      @NotNull AccessRequestConfigurationDto dto, @NotNull AccessRequestConfiguration entity) {
    entity.setProjectId(
        Objects.requireNonNull(projectManager.getProjectObjByKey(dto.getProjectKey())).getId());
    if (dto.getUsers() != null)
      entity.setUserKeys(
          dto.getUsers().stream().map(UserDto::getUserKey).collect(Collectors.joining(",")));
    if (dto.getGroups() != null) entity.setGroups(String.join(",", dto.getGroups()));
    if (dto.getProjectRoles() != null)
      entity.setProjectRoleIds(
          dto.getProjectRoles().stream()
              .map(role -> role.getId().toString())
              .collect(Collectors.joining(",")));
    if (dto.getUserFields() != null)
      entity.setUserFieldIds(
          dto.getUserFields().stream().map(UserFieldDto::getId).collect(Collectors.joining(",")));
    entity.setSendEmail(dto.isSendEmail());
    entity.setSendMessage(dto.isSendMessage());
  }

  @Override
  public @Nullable String mapDbField(@Nullable String s) {
    return null;
  }

  @Nullable
  public AccessRequestConfiguration getAccessRequestConfiguration(long projectId) {
    AccessRequestConfiguration[] configurations =
        ao.find(
            AccessRequestConfiguration.class, Query.select().where("PROJECT_ID = ?", projectId));
    return configurations.length > 0 ? configurations[0] : null;
  }

  private UserFieldDto buildUserFieldDto(String fieldId) {
    UserFieldDto userFieldDto;
    switch (fieldId) {
      case "reporter":
        userFieldDto =
            new UserFieldDto(
                AccessRequestConfigurationRepository.REPORTER,
                i18nHelper.getText("issue.field.reporter"));
        break;
      case "assignee":
        userFieldDto =
            new UserFieldDto(
                AccessRequestConfigurationRepository.ASSIGNEE,
                i18nHelper.getText("issue.field.assignee"));
        break;
      case "watchers":
        userFieldDto =
            new UserFieldDto(
                AccessRequestConfigurationRepository.WATCHERS,
                i18nHelper.getText("issue.field.watch"));
        break;
      default:
        userFieldDto =
            new UserFieldDto(
                Objects.requireNonNull(customFieldManager.getCustomFieldObject(fieldId)));
    }
    return userFieldDto;
  }
}
