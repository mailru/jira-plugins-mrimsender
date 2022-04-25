/* (C)2022 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.greenhopper.api.customfield.ManagedCustomFieldsService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.ThreadLocalSearcherCache;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.core.Pager;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.FillingIssueFieldState;

@Slf4j
public class EpicLinkValueHandler implements CreateIssueFieldValueHandler {

  private static final String CLASS_NAME =
      "com.atlassian.greenhopper.customfield.epiclink.EpicLinkCFType";

  private final SearchService searchService;
  private final I18nResolver i18nResolver;
  private final MessageFormatter messageFormatter;
  private final CustomField epicField;

  public EpicLinkValueHandler(
      SearchService searchService,
      I18nResolver i18nResolver,
      MessageFormatter messageFormatter,
      ManagedCustomFieldsService managedCustomFieldsService) {
    this.searchService = searchService;
    this.i18nResolver = i18nResolver;
    this.messageFormatter = messageFormatter;
    epicField = managedCustomFieldsService.getEpicNameCustomField().get();
  }

  @Override
  public String getClassName() {
    return CLASS_NAME;
  }

  @Override
  public String getInsertFieldMessage(
      Project project,
      IssueType issueType,
      FillingIssueFieldState state,
      ApplicationUser user,
      Locale locale) {
    try {
      List<Issue> epics = getEpics(state, user);

      if (epics.size() == 0) {
        return i18nResolver.getRawText(
            locale,
            "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.epicLinkSelect.empty");
      }
    } catch (SearchException e) {
      log.error(e.getLocalizedMessage(), e);
    }

    return i18nResolver.getRawText(
        locale, "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.epicLinkSelect.message");
  }

  @Override
  public List<List<InlineKeyboardMarkupButton>> getButtons(
      @NotNull Project project,
      @NotNull IssueType issueType,
      @NotNull FillingIssueFieldState state,
      @NotNull ApplicationUser user,
      @NotNull Locale locale) {

    try {
      List<Issue> epics = getEpics(state, user);
      if (epics.size() == 0) {
        return null;
      }

      Pager pager = state.getPager();
      pager.setTotal(epics.size());

      List<List<InlineKeyboardMarkupButton>> buttons =
          epics.stream()
              .skip((long) pager.getPage() * pager.getPageSize())
              .limit(pager.getPageSize())
              .map(
                  issue ->
                      ImmutableList.of(
                          InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                              String.format(
                                  "%s (%s)", issue.getCustomFieldValue(epicField), issue.getKey()),
                              String.join(
                                  "-",
                                  StateActionRuleType.SelectIssueCreationValue.getName(),
                                  String.format("key:%s", issue.getKey())))))
              .collect(Collectors.toList());
      @NotNull
      List<InlineKeyboardMarkupButton> pagerButtonsRow =
          messageFormatter.getPagerButtonsRow(locale, pager.hasPrev(), pager.hasNext());
      if (pagerButtonsRow.size() > 0) {
        buttons.add(pagerButtonsRow);
      }
      return buttons;
    } catch (SearchException e) {
      log.error(e.getLocalizedMessage(), e);
    }

    return null;
  }

  private List<Issue> getEpics(FillingIssueFieldState fillingFieldState, ApplicationUser user)
      throws SearchException {
    try {
      ThreadLocalSearcherCache.startSearcherContext();

      JqlQueryBuilder jqlBuilder = JqlQueryBuilder.newBuilder();
      JqlClauseBuilder jqlClauseBuilder = jqlBuilder.where().issueType().eq("epic");

      if (fillingFieldState.getInput() != null && fillingFieldState.getInput().length() > 0) {
        jqlClauseBuilder
            .and()
            .addClause(
                JqlQueryBuilder.newBuilder()
                    .where()
                    .summary()
                    .like(String.format("*%s*", fillingFieldState.getInput()))
                    .or()
                    .customField(epicField.getIdAsLong())
                    .like(String.format("*%s*", fillingFieldState.getInput()))
                    .buildClause());
      }

      return searchService
          .search(user, jqlClauseBuilder.buildQuery(), PagerFilter.getUnlimitedFilter())
          .getResults();
    } finally {
      ThreadLocalSearcherCache.stopAndCloseSearcherContext();
    }
  }

  @Override
  public boolean isSearchable() {
    return true;
  }
}
