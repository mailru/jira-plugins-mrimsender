/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.thread.JiraThreadLocalUtils;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import org.springframework.stereotype.Service;

@Service
public class IssueServiceImpl implements IssueService {

  private final IssueManager issueManager;
  private final PermissionManager permissionManager;
  private final WatcherManager watcherManager;
  private final SearchService searchService;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final String JIRA_BASE_URL;

  public IssueServiceImpl(
      @ComponentImport IssueManager issueManager,
      @ComponentImport PermissionManager permissionManager,
      @ComponentImport WatcherManager watcherManager,
      @ComponentImport SearchService searchService,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport ApplicationProperties applicationProperties) {
    this.issueManager = issueManager;
    this.permissionManager = permissionManager;
    this.watcherManager = watcherManager;
    this.searchService = searchService;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
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
}
