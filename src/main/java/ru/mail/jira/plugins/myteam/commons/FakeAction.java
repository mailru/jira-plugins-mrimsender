/* (C)2022 */
package ru.mail.jira.plugins.myteam.commons;

import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import webwork.action.Action;

public class FakeAction implements Action, OperationContext {
  private final Map<String, Object> fieldValuesHolder = new HashMap<>();
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final GlobalPermissionManager globalPermissionManager;

  public FakeAction(
      JiraAuthenticationContext jiraAuthenticationContext,
      GlobalPermissionManager globalPermissionManager) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.globalPermissionManager = globalPermissionManager;
  }

  @Override
  public String execute() throws Exception {
    return null;
  }

  @Override
  public Map<String, Object> getFieldValuesHolder() {
    return this.fieldValuesHolder;
  }

  @Override
  public IssueOperation getIssueOperation() {
    return IssueOperations.CREATE_ISSUE_OPERATION;
  }

  public Map<String, String> getErrors() {
    return Collections.emptyMap();
  }

  public String getText(String key) {
    return this.jiraAuthenticationContext.getI18nHelper().getText(key);
  }

  public boolean hasGlobalPermission(String key) {
    return this.globalPermissionManager.hasPermission(
        GlobalPermissionKey.of(key), this.jiraAuthenticationContext.getLoggedInUser());
  }
}
