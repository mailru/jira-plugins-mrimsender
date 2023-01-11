/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.controller;

import com.atlassian.jira.bc.user.search.UserSearchParams;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.customfields.CustomFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchContextFactory;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestConfigurationDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.AccessRequestDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.ProjectRoleDto;
import ru.mail.jira.plugins.myteam.accessrequest.controller.dto.UserFieldDto;
import ru.mail.jira.plugins.myteam.accessrequest.model.AccessRequestConfigurationRepository;
import ru.mail.jira.plugins.myteam.accessrequest.service.AccessRequestService;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;
import ru.mail.jira.plugins.myteam.controller.dto.GroupDto;
import ru.mail.jira.plugins.myteam.controller.dto.UserDto;

@Controller
@Path("/accessRequest")
@Produces(MediaType.APPLICATION_JSON)
public class AccessRequestController {
  private final CustomFieldManager customFieldManager;
  private final IssueManager issueManager;
  private final GroupManager groupManager;
  private final I18nHelper i18nHelper;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final ProjectManager projectManager;
  private final ProjectRoleManager projectRoleManager;
  private final SearchContextFactory searchContextFactory;
  private final UserSearchService userSearchService;
  private final AccessRequestService accessRequestService;
  private final PermissionHelper permissionHelper;

  public AccessRequestController(
      @ComponentImport CustomFieldManager customFieldManager,
      @ComponentImport IssueManager issueManager,
      @ComponentImport GroupManager groupManager,
      @ComponentImport I18nHelper i18nHelper,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport ProjectManager projectManager,
      @ComponentImport ProjectRoleManager projectRoleManager,
      @ComponentImport SearchContextFactory searchContextFactory,
      @ComponentImport UserSearchService userSearchService,
      AccessRequestService accessRequestService,
      PermissionHelper permissionHelper) {
    this.customFieldManager = customFieldManager;
    this.issueManager = issueManager;
    this.groupManager = groupManager;
    this.i18nHelper = i18nHelper;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.projectManager = projectManager;
    this.projectRoleManager = projectRoleManager;
    this.searchContextFactory = searchContextFactory;
    this.userSearchService = userSearchService;
    this.accessRequestService = accessRequestService;
    this.permissionHelper = permissionHelper;
  }

  @GET
  @Nullable
  public AccessRequestDto getAccessRequest(@QueryParam("issueKey") final String issueKey) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) throw new SecurityException();

    Issue issue = issueManager.getIssueObject(issueKey);
    if (issue == null) throw new IllegalArgumentException("Issue key must be specified.");

    return accessRequestService.getAccessRequest(loggedInUser, issue);
  }

  @POST
  public void sendAccessRequest(
      @QueryParam("issueKey") final String issueKey, AccessRequestDto accessRequestDto)
      throws MyteamServerErrorException, IOException {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) throw new SecurityException();

    Issue issue = issueManager.getIssueObject(issueKey);
    if (issue == null) throw new IllegalArgumentException("Issue key must be specified.");

    accessRequestService.sendAccessRequest(loggedInUser, issue, accessRequestDto);
  }

  @GET
  @Path("/configuration/{projectKey}")
  @Nullable
  public AccessRequestConfigurationDto getAccessRequestConfiguration(
      @PathParam("projectKey") final String projectKey) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) throw new SecurityException();

    Project project = projectManager.getProjectByCurrentKey(projectKey);
    if (project == null) throw new IllegalArgumentException("Project key must be specified.");

    if (!permissionHelper.isProjectAdmin(loggedInUser, project)) throw new SecurityException();

    return accessRequestService.getAccessRequestConfiguration(project);
  }

  @POST
  @Path("/configuration/{projectKey}")
  public void createAccessRequestConfiguration(
      @PathParam("projectKey") final String projectKey,
      AccessRequestConfigurationDto configurationDto) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    Project project = projectManager.getProjectByCurrentKey(projectKey);
    if (project == null) throw new IllegalArgumentException("Project key must be specified.");

    if (!permissionHelper.isProjectAdmin(loggedInUser, project)) throw new SecurityException();

    accessRequestService.createAccessRequestConfiguration(configurationDto);
  }

  @PUT
  @Path("/configuration/{projectKey}/{configurationId}")
  public void updateAccessRequestConfiguration(
      @PathParam("projectKey") final String projectKey,
      @PathParam("configurationId") int configurationId,
      AccessRequestConfigurationDto configurationDto) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    Project project = projectManager.getProjectByCurrentKey(projectKey);
    if (project == null) throw new IllegalArgumentException("Project key must be specified.");

    if (!permissionHelper.isProjectAdmin(loggedInUser, project)) throw new SecurityException();

    accessRequestService.updateAccessRequestConfiguration(configurationId, configurationDto);
  }

  @DELETE
  @Path("/configuration/{projectKey}/{configurationId}")
  public void deleteSubscription(
      @PathParam("projectKey") final String projectKey,
      @PathParam("configurationId") int configurationId) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    Project project = projectManager.getProjectByCurrentKey(projectKey);
    if (project == null) throw new IllegalArgumentException("Project key must be specified.");

    if (!permissionHelper.isProjectAdmin(loggedInUser, project)) throw new SecurityException();

    accessRequestService.deleteAccessRequestConfiguration(configurationId);
  }

  @GET
  @Path("/configuration/users")
  public List<UserDto> searchUsers(@QueryParam("query") final String query) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    UserSearchParams searchParams =
        new UserSearchParams(true, true, false, true, null, null, 10, true, false);
    return userSearchService.findUsers(StringUtils.trimToEmpty(query), searchParams).stream()
        .map(UserDto::new)
        .collect(Collectors.toList());
  }

  @GET
  @Path("/configuration/groups")
  public List<GroupDto> searchGroups(@QueryParam("query") final String query) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    return groupManager.getGroupNamesForUser(loggedInUser).stream()
        .filter(name -> name.contains(StringUtils.trimToEmpty(query)))
        .limit(10)
        .map(GroupDto::new)
        .collect(Collectors.toList());
  }

  @GET
  @Path("/configuration/projectRoles")
  public List<ProjectRoleDto> searchProjectRoles(
      @QueryParam("projectKey") final String projectKey, @QueryParam("query") final String query) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    Project project = projectManager.getProjectByCurrentKey(projectKey);
    if (project == null) throw new IllegalArgumentException("Project key must be specified.");

    return projectRoleManager.getProjectRoles(loggedInUser, project).stream()
        .filter(role -> role.getName().contains(StringUtils.trimToEmpty(query)))
        .map(ProjectRoleDto::new)
        .collect(Collectors.toList());
  }

  @GET
  @Path("/configuration/userFields")
  public List<UserFieldDto> searchUserFields(
      @QueryParam("projectKey") final String projectKey, @QueryParam("query") final String query) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    Project project = projectManager.getProjectByCurrentKey(projectKey);
    if (project == null) throw new IllegalArgumentException("Project key must be specified.");

    List<UserFieldDto> userFields = new ArrayList<>();
    userFields.add(
        new UserFieldDto(
            AccessRequestConfigurationRepository.REPORTER,
            i18nHelper.getText("issue.field.reporter")));
    userFields.add(
        new UserFieldDto(
            AccessRequestConfigurationRepository.ASSIGNEE,
            i18nHelper.getText("issue.field.assignee")));
    userFields.add(
        new UserFieldDto(
            AccessRequestConfigurationRepository.WATCHERS,
            i18nHelper.getText("issue.field.watch")));

    List<CustomField> fields =
        customFieldManager.getCustomFieldObjects(
            searchContextFactory.create(null, Collections.singletonList(project.getId()), null));
    userFields.addAll(
        fields.stream()
            .filter(this::isUserField)
            .map(UserFieldDto::new)
            .collect(Collectors.toList()));

    return userFields.stream()
        .filter(field -> field.getName().contains(StringUtils.trimToEmpty(query)))
        .collect(Collectors.toList());
  }

  private boolean isUserField(CustomField customField) {
    CustomFieldType customFieldType = customField.getCustomFieldType();
    return customFieldType instanceof com.atlassian.jira.issue.customfields.impl.UserCFType
        || customFieldType instanceof com.atlassian.jira.issue.customfields.impl.MultiUserCFType;
  }
}
