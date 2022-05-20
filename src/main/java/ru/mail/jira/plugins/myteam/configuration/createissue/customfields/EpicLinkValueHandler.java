/* (C)2022 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.greenhopper.api.customfield.ManagedCustomFieldsService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.ThreadLocalSearcherCache;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.core.Pager;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.FillingIssueFieldState;

public class EpicLinkValueHandler implements CreateIssueFieldValueHandler {

  private static final String CLASS_NAME =
      "com.atlassian.greenhopper.customfield.epiclink.EpicLinkCFType";

  private final SearchService searchService;
  private final I18nResolver i18nResolver;
  private final MessageFormatter messageFormatter;
  private final CustomField epicField;

  private final LoadingCache<EpicSearchData, SearchResults<Issue>> ttlEpicsCache;

  public EpicLinkValueHandler(
      SearchService searchService,
      I18nResolver i18nResolver,
      MessageFormatter messageFormatter,
      ManagedCustomFieldsService managedCustomFieldsService) {
    this.searchService = searchService;
    this.i18nResolver = i18nResolver;
    this.messageFormatter = messageFormatter;
    epicField = managedCustomFieldsService.getEpicNameCustomField().get();

    ttlEpicsCache =
        CacheBuilder.newBuilder()
            .expireAfterAccess(20, TimeUnit.SECONDS)
            .build(CacheLoader.from(this::getEpics));
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
    SearchResults<Issue> epics;
    try {
      epics =
          ttlEpicsCache.get(
              EpicSearchData.builder()
                  .q(state.getInput())
                  .user(user)
                  .project(project)
                  .page(state.getPager().getPage())
                  .pageSize(state.getPager().getPageSize())
                  .build());
    } catch (ExecutionException e) {
      return null;
    }

    if (epics == null || epics.getResults().size() == 0) {
      return i18nResolver.getRawText(
          locale, "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.epicLinkSelect.empty");
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
    SearchResults<Issue> epics;
    try {
      epics =
          ttlEpicsCache.get(
              EpicSearchData.builder()
                  .q(state.getInput())
                  .user(user)
                  .project(project)
                  .page(state.getPager().getPage())
                  .pageSize(state.getPager().getPageSize())
                  .build());
    } catch (ExecutionException e) {
      return null;
    }
    if (epics == null || epics.getResults().size() == 0) {
      return null;
    }

    Pager pager = state.getPager();
    pager.setTotal(epics.getTotal());

    List<List<InlineKeyboardMarkupButton>> buttons =
        epics.getResults().stream()
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
  }

  @Nullable
  private SearchResults<Issue> getEpics(EpicSearchData data) {
    try {
      ThreadLocalSearcherCache.startSearcherContext();

      JqlQueryBuilder jqlBuilder = JqlQueryBuilder.newBuilder();
      JqlClauseBuilder jqlClauseBuilder = jqlBuilder.where().issueType().eq("epic");

      if (data.getQ() != null && data.getQ().length() > 0) {
        jqlClauseBuilder
            .and()
            .addClause(
                JqlQueryBuilder.newBuilder()
                    .where()
                    .summary()
                    .like(String.format("*%s*", data.getQ()))
                    .or()
                    .customField(epicField.getIdAsLong())
                    .like(String.format("*%s*", data.getQ()))
                    .buildClause());
      } else {
        jqlClauseBuilder.and().project().eq(data.getProject().getKey());
      }

      return searchService.search(
          data.getUser(),
          jqlClauseBuilder.buildQuery(),
          PagerFilter.newPageAlignedFilter(
              data.getPage() * data.getPageSize(), data.getPageSize()));
    } catch (SearchException e) {
      return null;
    } finally {
      ThreadLocalSearcherCache.stopAndCloseSearcherContext();
    }
  }

  @Override
  public boolean isSearchable() {
    return true;
  }

  @Getter
  @Builder
  @EqualsAndHashCode
  private static class EpicSearchData {
    private final ApplicationUser user;
    private final Project project;
    private final String q;
    private final int page;
    private final int pageSize;
  }
}
