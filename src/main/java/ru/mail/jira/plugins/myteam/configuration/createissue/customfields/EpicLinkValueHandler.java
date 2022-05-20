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
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.mail.jira.plugins.myteam.configuration.createissue.FieldInputMessageInfo;
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
  public FieldInputMessageInfo getMessageInfo(
      @NotNull Project project,
      @NotNull IssueType issueType,
      @NotNull ApplicationUser user,
      @NotNull Locale locale,
      @NotNull FillingIssueFieldState state) {
    @Nullable
    SearchResults<Issue> epics =
        getEpics(
            EpicSearchData.builder()
                .q(state.getInput())
                .user(user)
                .project(project)
                .page(state.getPager().getPage())
                .pageSize(state.getPager().getPageSize())
                .build());

    return FieldInputMessageInfo.builder()
        .message(getInsertFieldMessage(locale, epics))
        .buttons(getButtons(state, locale, epics))
        .build();
  }

  public String getInsertFieldMessage(Locale locale, @Nullable SearchResults<Issue> epics) {

    if (epics == null || epics.getResults().size() == 0) {
      return i18nResolver.getRawText(
          locale, "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.epicLinkSelect.empty");
    }

    return i18nResolver.getRawText(
        locale, "ru.mail.jira.plugins.myteam.messageFormatter.createIssue.epicLinkSelect.message");
  }

  public List<List<InlineKeyboardMarkupButton>> getButtons(
      @NotNull FillingIssueFieldState state,
      @NotNull Locale locale,
      @Nullable SearchResults<Issue> epics) {
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
