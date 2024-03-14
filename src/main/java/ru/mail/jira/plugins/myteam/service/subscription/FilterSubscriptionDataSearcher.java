/* (C)2024 */
package ru.mail.jira.plugins.myteam.service.subscription;

import com.atlassian.gzipfilter.org.apache.commons.lang.StringEscapeUtils;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.group.search.GroupPickerSearchService;
import com.atlassian.jira.bc.user.search.UserSearchParams;
import com.atlassian.jira.bc.user.search.UserSearchService;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.sharing.SharedEntityColumn;
import com.atlassian.jira.sharing.index.QueryBuilder;
import com.atlassian.jira.sharing.search.SharedEntitySearchContext;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionPermissionsDto;
import ru.mail.jira.plugins.myteam.controller.dto.GroupDto;
import ru.mail.jira.plugins.myteam.controller.dto.JqlFilterDto;
import ru.mail.jira.plugins.myteam.controller.dto.UserDto;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Component
public class FilterSubscriptionDataSearcher {
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final SearchRequestService searchRequestService;
  private final UserSearchService userSearchService;
  private final GroupPickerSearchService groupPickerSearchService;

  private final GroupManager groupManager;
  private final PermissionHelper permissionHelper;
  private final PluginData pluginData;

  public FilterSubscriptionDataSearcher(
      @ComponentImport final JiraAuthenticationContext jiraAuthenticationContext,
      @ComponentImport final SearchRequestService searchRequestService,
      @ComponentImport final UserSearchService userSearchService,
      @ComponentImport final GroupPickerSearchService groupPickerSearchService,
      @ComponentImport final GroupManager groupManager,
      final PermissionHelper permissionHelper,
      final PluginData pluginData) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.searchRequestService = searchRequestService;
    this.userSearchService = userSearchService;
    this.groupPickerSearchService = groupPickerSearchService;
    this.groupManager = groupManager;
    this.permissionHelper = permissionHelper;
    this.pluginData = pluginData;
  }

  public List<JqlFilterDto> searchJqlFilters(final String query) {
    final ApplicationUser loggedInUser = checkLoggedInUser();

    final SharedEntitySearchParameters searchParameters =
        new SharedEntitySearchParametersBuilder()
            .setName(
                StringUtils.isBlank(query)
                    ? StringUtils.EMPTY
                    : StringEscapeUtils.escapeJava(query))
            .setTextSearchMode(SharedEntitySearchParameters.TextSearchMode.WILDCARD)
            .setEntitySearchContext(SharedEntitySearchContext.USE)
            .setSortColumn(SharedEntityColumn.OWNER, true)
            .toSearchParameters();

    QueryBuilder.validate(searchParameters);

    return searchRequestService
        .search(new JiraServiceContextImpl(loggedInUser), searchParameters, 0, 100)
        .getResults()
        .stream()
        .map(searchRequest -> new JqlFilterDto(searchRequest, loggedInUser))
        .collect(Collectors.toList());
  }

  public List<UserDto> searchUsers(final String query) {
    final ApplicationUser loggedInUser = checkLoggedInUser();

    if (!permissionHelper.isJiraAdmin(loggedInUser)) {
      return Collections.singletonList(new UserDto(loggedInUser));
    }

    UserSearchParams searchParams =
        new UserSearchParams(true, true, false, true, null, null, 10, true, false);

    return userSearchService.findUsers(query, searchParams).stream()
        .map(UserDto::new)
        .collect(Collectors.toList());
  }

  public List<GroupDto> searchGroups(final String query) {
    final ApplicationUser loggedInUser = checkLoggedInUser();

    if (permissionHelper.isJiraAdmin(loggedInUser)) {
      return groupPickerSearchService.findGroups(query).stream()
          .limit(10)
          .map(GroupDto::new)
          .collect(Collectors.toList());
    } else {
      return groupManager.getGroupNamesForUser(loggedInUser).stream()
          .filter(name -> name.contains(query) && !isGroupExcluded(name))
          .limit(10)
          .map(GroupDto::new)
          .collect(Collectors.toList());
    }
  }

  public FilterSubscriptionPermissionsDto getPermissions() {
    final ApplicationUser loggedInUser = checkLoggedInUser();
    return new FilterSubscriptionPermissionsDto(permissionHelper.isJiraAdmin(loggedInUser));
  }

  private boolean isGroupExcluded(String group) {
    return pluginData.getSubscriptionsExcludingGroups().contains(group);
  }

  @NotNull
  private ApplicationUser checkLoggedInUser() {
    final ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }
    return loggedInUser;
  }
}
