/* (C)2021 */
package ru.mail.jira.plugins.myteam.service.impl;

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
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.naming.NoPermissionException;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.myteam.protocol.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.rulesengine.core.Utils;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IssueWatchingException;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.ProjectBannedException;
import ru.mail.jira.plugins.myteam.service.IssueService;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Service
public class IssueServiceImpl implements IssueService {

  private final IssueManager issueManager;
  private final PermissionManager permissionManager;
  private final WatcherManager watcherManager;
  private final SearchService searchService;
  private final CommentManager commentManager;
  private final ProjectManager projectManager;
  private final IssueTypeSchemeManager issueTypeSchemeManager;
  private final IssueTypeManager issueTypeManager;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final Utils utils;
  private final PluginData pluginData;
  private final String JIRA_BASE_URL;

  public IssueServiceImpl(
      Utils utils,
      @ComponentImport IssueManager issueManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport WatcherManager watcherManager,
      @ComponentImport SearchService searchService,
      @ComponentImport CommentManager commentManager,
      @ComponentImport ProjectManager projectManager,
      @ComponentImport IssueTypeSchemeManager issueTypeSchemeManager,
      @ComponentImport IssueTypeManager issueTypeManager,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport ApplicationProperties applicationProperties,
      PluginData pluginData) {
    this.issueManager = issueManager;
    this.permissionManager = permissionManager;
    this.watcherManager = watcherManager;
    this.searchService = searchService;
    this.commentManager = commentManager;
    this.projectManager = projectManager;
    this.issueTypeSchemeManager = issueTypeSchemeManager;
    this.issueTypeManager = issueTypeManager;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.utils = utils;
    this.pluginData = pluginData;
    this.JIRA_BASE_URL = applicationProperties.getString(APKeys.JIRA_BASEURL);
  }

  @Override
  public Issue getIssueByUser(String issueKey, ApplicationUser user) {
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(user);
      Issue issue = issueManager.getIssueByKeyIgnoreCase(issueKey);
      if (issue != null) {
        if (permissionManager.hasPermission(ProjectPermissions.BROWSE_PROJECTS, issue, user)) {
          return issue;
        } else
          throw new IssuePermissionException(
              String.format("User has no permissions to view issue %s", issueKey));
      } else throw new IssueNotFoundException(String.format("Issue %s not found", issueKey));
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
    }
  }

  @Override
  public boolean isUserWatching(Issue issue, ApplicationUser user) {
    return watcherManager.isWatching(user, issue);
  }

  @Override
  public String getJiraBaseUrl() {
    return JIRA_BASE_URL;
  }

  @Override
  public SearchResults<Issue> SearchByJql(String jql, ApplicationUser user, int page, int pageSize)
      throws SearchException, ParseException {
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
  public void watchIssue(String issueKey, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException {
    Issue issue = getIssueByUser(issueKey, user);
    if (watcherManager.isWatching(user, issue)) {
      throw new IssueWatchingException(
          String.format("Issue with key %s already watched", issueKey));
    } else {
      watcherManager.startWatching(user, issue);
    }
  }

  @Override
  public void watchIssue(Issue issue, ApplicationUser user) {
    if (!watcherManager.isWatching(user, issue)) {
      watcherManager.startWatching(user, issue);
    }
  }

  @Override
  public void unwatchIssue(String issueKey, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException {
    Issue issue = getIssueByUser(issueKey, user);
    if (!watcherManager.isWatching(user, issue)) {
      throw new IssueWatchingException(
          String.format("Issue with key %s already unwatched", issueKey));
    } else {
      watcherManager.stopWatching(user, issue);
    }
  }

  @Override
  public void commentIssue(String issueKey, ApplicationUser user, ChatMessageEvent event)
      throws NoPermissionException {
    JiraThreadLocalUtils.preCall();
    try {
      Issue commentedIssue = issueManager.getIssueByCurrentKey(issueKey);
      if (user != null && commentedIssue != null) {
        if (permissionManager.hasPermission(
            ProjectPermissions.ADD_COMMENTS, commentedIssue, user)) {
          commentManager.create(
              commentedIssue,
              user,
              utils.convertToJiraCommentStyle(event, user, commentedIssue),
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
  public List<Project> getAllowedProjects() {
    return projectManager.getProjects().stream()
        .filter(proj -> !isProjectExcluded(proj.getId()))
        .collect(Collectors.toList());
  }

  @Override
  public Project getProject(String projectKey, ApplicationUser user)
      throws PermissionException, ProjectBannedException {
    Project selectedProject = projectManager.getProjectByCurrentKeyIgnoreCase(projectKey);
    if (selectedProject == null) {
      return null;
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
  public Collection<IssueType> getProjectIssueTypes(Project project, ApplicationUser user) {
    ApplicationUser contextPrevUser = jiraAuthenticationContext.getLoggedInUser();
    try {
      jiraAuthenticationContext.setLoggedInUser(user);
      return issueTypeSchemeManager.getNonSubTaskIssueTypesForProject(project);
    } finally {
      jiraAuthenticationContext.setLoggedInUser(contextPrevUser);
    }
  }

  @Override
  public IssueType getIssueType(String id) {
    return issueTypeManager.getIssueType(id);
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
  public List<Comment> getIssueComments(String issueKey, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException {
    JiraThreadLocalUtils.preCall();
    try {
      return commentManager.getComments(getIssueByUser(issueKey, user));
    } finally {
      JiraThreadLocalUtils.postCall();
    }
  }

  private boolean isProjectExcluded(Long projectId) {
    return pluginData.getExcludingProjectIds().contains(projectId);
  }
}
