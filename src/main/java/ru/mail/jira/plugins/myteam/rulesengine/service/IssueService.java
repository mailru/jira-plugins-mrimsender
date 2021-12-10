/* (C)2021 */
package ru.mail.jira.plugins.myteam.rulesengine.service;

import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.user.ApplicationUser;
import ru.mail.jira.plugins.myteam.rulesengine.models.exceptions.IssueWatchingException;

public interface IssueService {

  Issue getIssueByUser(String issueKey, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException;

  Issue getIssue(String issueKey) throws IssueNotFoundException;

  boolean isUserWatching(Issue issue, ApplicationUser user);

  String getJiraBaseUrl();

  SearchResults<Issue> SearchByJql(String jql, ApplicationUser user, int page, int pageSize)
      throws SearchException, ParseException;

  void watchIssue(String issueKey, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException;

  void unwatchIssue(String issueKey, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException;
}
