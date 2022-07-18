/* (C)2021 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.IssueTypeManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.comments.CommentManager;
import com.atlassian.jira.issue.fields.config.manager.IssueTypeSchemeManager;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.workflow.WorkflowManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import com.opensymphony.workflow.loader.ActionDescriptor;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.naming.NoPermissionException;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.AssigneeChangeValidationException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IssueTransitionException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IssueWatchingException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.ProjectBannedException;
import ru.mail.jira.plugins.myteam.component.IssueTextConverter;
import ru.mail.jira.plugins.myteam.component.UserData;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Service
public class IssueServiceImpl implements IssueService {

  private final com.atlassian.jira.bc.issue.IssueService jiraIssueService;
  private final IssueManager issueManager;
  private final PermissionManager permissionManager;
  private final WatcherManager watcherManager;
  private final SearchService searchService;
  private final CommentManager commentManager;
  private final ProjectManager projectManager;
  private final IssueTypeSchemeManager issueTypeSchemeManager;
  private final IssueTypeManager issueTypeManager;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final IssueTextConverter issueTextConverter;
  private final WorkflowManager workflowManager;
  private final UserData userData;
  private final PluginData pluginData;
  private final String JIRA_BASE_URL;

  public IssueServiceImpl(
      @ComponentImport com.atlassian.jira.bc.issue.IssueService jiraIssueService,
      @ComponentImport IssueManager issueManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport WatcherManager watcherManager,
      @ComponentImport SearchService searchService,
      @ComponentImport CommentManager commentManager,
      @ComponentImport ProjectManager projectManager,
      @ComponentImport IssueTypeSchemeManager issueTypeSchemeManager,
      @ComponentImport IssueTypeManager issueTypeManager,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport WorkflowManager workflowManager,
      @ComponentImport ApplicationProperties applicationProperties,
      UserData userData,
      IssueTextConverter issueTextConverter,
      PluginData pluginData) {
    this.jiraIssueService = jiraIssueService;
    this.issueManager = issueManager;
    this.permissionManager = permissionManager;
    this.watcherManager = watcherManager;
    this.searchService = searchService;
    this.commentManager = commentManager;
    this.projectManager = projectManager;
    this.issueTypeSchemeManager = issueTypeSchemeManager;
    this.issueTypeManager = issueTypeManager;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.workflowManager = workflowManager;
    this.userData = userData;
    this.issueTextConverter = issueTextConverter;
    this.pluginData = pluginData;
    this.JIRA_BASE_URL = applicationProperties.getString(APKeys.JIRA_BASEURL);
  }

  @Override
  public Issue getIssueByUser(String issueKey) {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

    return getIssueByUser(issueKey, user);
  }

  @Override
  public boolean isUserWatching(Issue issue) {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    return watcherManager.isWatching(user, issue);
  }

  @Override
  public String getJiraBaseUrl() {
    return JIRA_BASE_URL;
  }

  @Override
  public SearchResults<Issue> SearchByJql(String jql, int page, int pageSize)
      throws SearchException, ParseException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    JiraThreadLocalUtils.preCall();

    SearchService.ParseResult parseResult = searchService.parseQuery(user, jql);
    if (parseResult.isValid()) {

      Query jqlQuery = parseResult.getQuery();
      Query sanitizedJql = searchService.sanitiseSearchQuery(user, jqlQuery);
      PagerFilter<Issue> pagerFilter = new PagerFilter<>(page * pageSize, pageSize);
      SearchResults<Issue> res = searchService.search(user, sanitizedJql, pagerFilter);

      JiraThreadLocalUtils.postCall();
      return res;
    } else {
      JiraThreadLocalUtils.postCall();
      throw new ParseException("Incorrect jql expression");
    }
  }

  @Override
  public void watchIssue(String issueKey)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    Issue issue = getIssueByUser(issueKey, user);
    watchIssue(issue, user);
  }

  @Override
  public void watchIssue(Issue issue, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException {
    if (watcherManager.isWatching(user, issue)) {
      throw new IssueWatchingException(
          String.format("Issue with key %s already watched", issue.getKey()));
    } else {
      watcherManager.startWatching(user, issue);
    }
  }

  @Override
  public void unwatchIssue(String issueKey)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    Issue issue = getIssueByUser(issueKey);
    if (!watcherManager.isWatching(user, issue)) {
      throw new IssueWatchingException(
          String.format("Issue with key %s already unwatched", issueKey));
    } else {
      watcherManager.stopWatching(user, issue);
    }
  }

  @Override
  public void commentIssue(String issueKey, ChatMessageEvent event) throws NoPermissionException {
    JiraThreadLocalUtils.preCall();
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    try {
      Issue commentedIssue = issueManager.getIssueByCurrentKey(issueKey);
      if (user != null && commentedIssue != null) {
        if (permissionManager.hasPermission(
            ProjectPermissions.ADD_COMMENTS, commentedIssue, user)) {
          commentManager.create(
              commentedIssue,
              user,
              issueTextConverter.convertToJiraCommentStyle(event, user, commentedIssue),
              true);
        } else {
          throw new NoPermissionException();
        }
      }
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  @Override
  public void changeIssueStatus(Issue issue, int transitionId) throws IssueTransitionException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    com.atlassian.jira.bc.issue.IssueService.TransitionValidationResult validationResult =
        jiraIssueService.validateTransition(
            user, issue.getId(), transitionId, jiraIssueService.newIssueInputParameters());

    if (!validationResult.isValid()) {
      throw new IssueTransitionException(
          "Error due validation issue transition", validationResult.getErrorCollection());
    }

    com.atlassian.jira.bc.issue.IssueService.IssueResult res =
        jiraIssueService.transition(user, validationResult);

    if (!res.isValid()) {
      throw new IssueTransitionException(
          "Error due changing issue status", validationResult.getErrorCollection());
    }
  }

  @Override
  public List<Project> getAllowedProjects() {
    return projectManager.getProjects().stream()
        .filter(proj -> !isProjectExcluded(proj.getId()))
        .collect(Collectors.toList());
  }

  @Override
  public Project getProject(String projectKey) throws PermissionException, ProjectBannedException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();

    Project selectedProject = projectManager.getProjectByCurrentKeyIgnoreCase(projectKey);
    if (selectedProject == null) {
      throw new PermissionException();
    }
    if (isProjectExcluded(selectedProject.getId())) {
      throw new ProjectBannedException(String.format("Project with key %s is banned", projectKey));
    }

    if (!permissionManager.hasPermission(ProjectPermissions.CREATE_ISSUES, selectedProject, user)) {
      throw new PermissionException();
    }
    return selectedProject;
  }

  @Override
  public Collection<IssueType> getProjectIssueTypes(Project project) {
    return issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(project);
  }

  @Override
  public IssueType getIssueType(String id) {
    return issueTypeManager.getIssueType(id);
  }

  @Override
  public Collection<ActionDescriptor> getIssueTransitions(String issueKey) {
    return workflowManager.getWorkflow(getIssueByUser(issueKey)).getAllActions();
  }

  @Override
  public boolean changeIssueAssignee(String issueKey, String assigneeMyteamLogin)
      throws UserNotFoundException, AssigneeChangeValidationException {
    @Nullable ApplicationUser assignee = userData.getUserByMrimLogin(assigneeMyteamLogin);
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    if (assignee == null) {
      throw new UserNotFoundException(assigneeMyteamLogin);
    }
    MutableIssue issue = jiraIssueService.getIssue(user, issueKey).getIssue();

    if (issue == null) {
      throw new IssueNotFoundException();
    }
    JiraThreadLocalUtils.preCall();
    // need here to because issueService use authenticationContext
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(user);
      com.atlassian.jira.bc.issue.IssueService.AssignValidationResult assignResult =
          jiraIssueService.validateAssign(user, issue.getId(), assignee.getUsername());

      if (!assignResult.isValid()) {
        throw new AssigneeChangeValidationException(
            "Unable to change issue assignee", assignResult.getErrorCollection());
      }
      return jiraIssueService.assign(user, assignResult).isValid();
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
      JiraThreadLocalUtils.postCall();
    }
  }

  @Override
  public Issue getIssue(String issueKey) throws IssueNotFoundException {
    Issue issue = issueManager.getIssueByKeyIgnoreCase(issueKey);
    if (issue == null) {
      throw new IssueNotFoundException(String.format("Issue with key %s was not found", issueKey));
    }
    return issue;
  }

  @Override
  public List<Comment> getIssueComments(String issueKey)
      throws IssuePermissionException, IssueNotFoundException {
    JiraThreadLocalUtils.preCall();
    try {
      return commentManager.getComments(getIssueByUser(issueKey));
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  private boolean isProjectExcluded(Long projectId) {
    return pluginData.getExcludingProjectIds().contains(projectId);
  }

  private Issue getIssueByUser(String issueKey, ApplicationUser user) {
    Issue issue = issueManager.getIssueByKeyIgnoreCase(issueKey);
    if (issue != null) {
      if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user)) {
        return issue;
      } else
        throw new IssuePermissionException(
            String.format("User has no permissions to view issue %s", issueKey));
    } else {
      throw new IssueNotFoundException(String.format("Issue %s not found", issueKey));
    }
  }
}
