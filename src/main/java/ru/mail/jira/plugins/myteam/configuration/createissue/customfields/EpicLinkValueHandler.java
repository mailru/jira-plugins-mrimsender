/* (C)2022 */
package ru.mail.jira.plugins.myteam.configuration.createissue.customfields;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.index.ThreadLocalSearcherCache;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.protocol.MessageFormatter;
import ru.mail.jira.plugins.myteam.rulesengine.core.Pager;
import ru.mail.jira.plugins.myteam.rulesengine.models.ruletypes.StateActionRuleType;
import ru.mail.jira.plugins.myteam.rulesengine.states.issuecreation.FillingIssueFieldState;

@Slf4j
public class EpicLinkValueHandler implements CreateIssueFieldValueHandler {

  private final SearchService searchService;
  private final MessageFormatter messageFormatter;

  public EpicLinkValueHandler(SearchService searchService, MessageFormatter messageFormatter) {
    this.searchService = searchService;
    this.messageFormatter = messageFormatter;
  }

  @Override
  public String getClassName() {
    return "com.atlassian.greenhopper.customfield.epiclink.EpicLinkCFType";
  }

  @Override
  public String getInsertFieldMessage(FillingIssueFieldState field, Locale locale) {
    return "Введите название эпика";
  }

  // key:KB-1
  @Override
  public List<List<InlineKeyboardMarkupButton>> getButtons(
      Project project,
      IssueType issueType,
      FillingIssueFieldState fillingFieldState,
      ApplicationUser user,
      Locale locale) {

    JqlQueryBuilder jqlBuilder = JqlQueryBuilder.newBuilder();
    JqlClauseBuilder jqlClauseBuilder = jqlBuilder.where().issueType().eq("epic");

    if (fillingFieldState.getInput() != null && fillingFieldState.getInput().length() > 0) {
      jqlClauseBuilder.and().summary().like(String.format("*%s*", fillingFieldState.getInput()));
    }

    try {
      ThreadLocalSearcherCache.startSearcherContext();
      List<Issue> res =
          searchService
              .search(user, jqlClauseBuilder.buildQuery(), PagerFilter.getUnlimitedFilter())
              .getResults();

      Pager pager = fillingFieldState.getPager();
      pager.setTotal(res.size());

      List<List<InlineKeyboardMarkupButton>> buttons =
          res.stream()
              .skip((long) pager.getPage() * pager.getPageSize())
              .limit(pager.getPageSize())
              .map(
                  issue ->
                      ImmutableList.of(
                          InlineKeyboardMarkupButton.buildButtonWithoutUrl(
                              String.format("%s (%s)", issue.getSummary(), issue.getKey()),
                              String.join(
                                  "-",
                                  StateActionRuleType.SelectIssueCreationValue.getName(),
                                  String.format("key:%s", issue.getKey())))))
              .collect(Collectors.toList());
      buttons.add(messageFormatter.getPagerButtonsRow(locale, pager.hasPrev(), pager.hasNext()));
      return buttons;
    } catch (SearchException e) {
      log.error(e.getLocalizedMessage(), e);
    } finally {
      ThreadLocalSearcherCache.stopAndCloseSearcherContext();
    }

    return null;
  }

  @Override
  public boolean isSearchable() {
    return true;
  }
}
