/* (C)2022 */
package ru.mail.jira.plugins.myteam.service.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.commons.FakeAction;
import ru.mail.jira.plugins.myteam.service.HtmlFieldFactory;
import ru.mail.jira.plugins.myteam.service.dto.FieldDto;

@Component
public class HtmlFieldFactoryImpl implements HtmlFieldFactory {

  private static final ImmutableSet<String> SKIP_FIELDS =
      ImmutableSet.of(
          "issuetype", "summary", "description", "reporter", "assignee", "attachment", "labels");

  private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final GlobalPermissionManager globalPermissionManager;
  private final FieldLayoutManager fieldLayoutManager;
  private final FieldManager fieldManager;

  @Autowired
  public HtmlFieldFactoryImpl(
      @ComponentImport IssueTypeScreenSchemeManager issueTypeScreenSchemeManager,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport GlobalPermissionManager globalPermissionManager,
      @ComponentImport FieldLayoutManager fieldLayoutManager,
      @ComponentImport FieldManager fieldManager) {
    this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.globalPermissionManager = globalPermissionManager;
    this.fieldLayoutManager = fieldLayoutManager;
    this.fieldManager = fieldManager;
  }

  @Override
  public ArrayList<FieldDto> getFields(Issue issue, boolean requiredOnly) {

    List<FieldScreenLayoutItem> fields = getFields(issue);

    ArrayList<FieldDto> all = new ArrayList<>();
    for (FieldScreenLayoutItem fli : fields) {
      OrderableField field = fli.getOrderableField();
      FieldLayoutItem fieldLayoutItem =
          fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem(field);

      if (requiredOnly && !fieldLayoutItem.isRequired()) {
        continue;
      }
      String html = null;
      if (field.isShown(issue)) {
        html = StringUtils.trimToNull(getFieldHtmlView(fli, issue));
      }
      FieldDto fieldDto = new FieldDto(field.getId(), field.getName(), null, null, html);
      all.add(fieldDto);
    }
    return all;
  }

  private String getFieldHtmlView(FieldScreenLayoutItem fsli, Issue issue) {
    OrderableField field = fsli.getOrderableField();
    FieldLayoutItem fieldLayoutItem =
        fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem(field);

    return field.getEditHtml(
        fieldLayoutItem,
        new FakeAction(jiraAuthenticationContext, globalPermissionManager),
        new FakeAction(jiraAuthenticationContext, globalPermissionManager),
        issue,
        MapBuilder.<String, Object>newBuilder("edit_issue", true).toMutableMap());
  }

  private List<FieldScreenLayoutItem> getFields(Issue issue) {
    FieldScreenScheme fieldScreenScheme = issueTypeScreenSchemeManager.getFieldScreenScheme(issue);
    FieldScreen fieldScreen =
        fieldScreenScheme.getFieldScreen(IssueOperations.CREATE_ISSUE_OPERATION);
    return fieldScreen.getTabs().stream()
        .flatMap(screen -> screen.getFieldScreenLayoutItems().stream())
        .filter(item -> !SKIP_FIELDS.contains(item.getFieldId()))
        .filter(item -> fieldManager.getField(item.getFieldId()) != null)
        .collect(Collectors.toList());
  }
}
