/* (C)2022 */
package ru.mail.jira.plugins.myteam.accessrequest.filter;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.io.IOException;
import javax.annotation.Nullable;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.accessrequest.service.AccessRequestService;

@Component
public class AccessRequestFilter implements Filter {
  private final IssueManager issueManager;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final PermissionManager permissionManager;
  private final AccessRequestService accessRequestService;

  @Autowired
  public AccessRequestFilter(
      @ComponentImport IssueManager issueManager,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport PermissionManager permissionManager,
      AccessRequestService accessRequestService) {
    this.issueManager = issueManager;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.permissionManager = permissionManager;
    this.accessRequestService = accessRequestService;
  }

  @Override
  public void init(FilterConfig filterConfig) {}

  @Override
  public void destroy() {}

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
      throws IOException, ServletException {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (servletRequest instanceof HttpServletRequest
        && servletResponse instanceof HttpServletResponse) {
      HttpServletRequest request = (HttpServletRequest) servletRequest;
      HttpServletResponse response = (HttpServletResponse) servletResponse;

      if (loggedInUser == null) {
        chain.doFilter(servletRequest, servletResponse);
        return;
      }

      Issue issue = parseIssueFromLink(request.getRequestURI());
      if (issue == null) {
        chain.doFilter(servletRequest, servletResponse);
        return;
      }

      if (permissionManager.hasPermission(
          ProjectPermissions.BROWSE_PROJECTS, issue, loggedInUser)) {
        chain.doFilter(servletRequest, servletResponse);
        return;
      }

      if (issue.getProjectObject() != null
          && accessRequestService.getAccessRequestConfiguration(issue.getProjectObject()) != null) {
        response.sendRedirect(
            request.getContextPath() + "/secure/AccessRequest.jspa?issueKey=" + issue.getKey());
        return;
      }
    }
    chain.doFilter(servletRequest, servletResponse);
  }

  @Nullable
  private Issue parseIssueFromLink(String link) {
    String issueKey = null;
    if (link.contains("/browse/")) {
      String parsedKey = StringUtils.substringAfter(link, "/browse/");
      if (parsedKey.length() != 0 && parsedKey.contains("-")) {
        issueKey = parsedKey;
      }
    }
    return issueManager.getIssueByCurrentKey(issueKey);
  }
}
