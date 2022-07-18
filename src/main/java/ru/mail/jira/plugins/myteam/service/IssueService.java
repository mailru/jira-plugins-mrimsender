/* (C)2021 */
package ru.mail.jira.plugins.myteam.service;

import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comments.Comment;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.opensymphony.workflow.loader.ActionDescriptor;
import java.util.Collection;
import java.util.List;
import javax.naming.NoPermissionException;
import ru.mail.jira.plugins.myteam.bot.events.ChatMessageEvent;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.AssigneeChangeValidationException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IssueTransitionException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.IssueWatchingException;
import ru.mail.jira.plugins.myteam.bot.rulesengine.models.exceptions.ProjectBannedException;

public interface IssueService {

  Issue getIssueByUser(String issueKey) throws IssuePermissionException, IssueNotFoundException;

  Issue getIssue(String issueKey) throws IssueNotFoundException;

  boolean isUserWatching(Issue issue);

  String getJiraBaseUrl();

  SearchResults<Issue> SearchByJql(String jql,  int page, int pageSize)
      throws SearchException, ParseException;

  void watchIssue(String issueKey)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException;

  void watchIssue(Issue issueKey, ApplicationUser user)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException;

  void unwatchIssue(String issueKey)
      throws IssuePermissionException, IssueNotFoundException, IssueWatchingException;

  void commentIssue(String issueKey, ChatMessageEvent event) throws NoPermissionException;

  void changeIssueStatus(Issue issue, int transitionId) throws IssueTransitionException;

  List<Project> getAllowedProjects();

  List<Comment> getIssueComments(String issueKey)
      throws IssuePermissionException, IssueNotFoundException;

  Project getProject(String projectKey) throws PermissionException, ProjectBannedException;

  Collection<IssueType> getProjectIssueTypes(Project project);

  IssueType getIssueType(String id);

  Collection<ActionDescriptor> getIssueTransitions(String issueKey)
      throws IssuePermissionException, IssueNotFoundException;

  boolean changeIssueAssignee(String issueKey, String userMention)
      throws UserNotFoundException, AssigneeChangeValidationException;
}
