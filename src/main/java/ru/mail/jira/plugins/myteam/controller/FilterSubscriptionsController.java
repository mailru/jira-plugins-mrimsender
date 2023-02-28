/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller;

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
import com.atlassian.jira.sharing.search.SearchParseException;
import com.atlassian.jira.sharing.search.SharedEntitySearchContext;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.commons.RestFieldException;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionDto;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionPermissionsDto;
import ru.mail.jira.plugins.myteam.controller.dto.GroupDto;
import ru.mail.jira.plugins.myteam.controller.dto.JqlFilterDto;
import ru.mail.jira.plugins.myteam.controller.dto.UserDto;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;
import ru.mail.jira.plugins.myteam.db.model.RecipientsType;
import ru.mail.jira.plugins.myteam.db.repository.FilterSubscriptionRepository;
import ru.mail.jira.plugins.myteam.service.FilterSubscriptionService;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Controller
@Path("/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
public class FilterSubscriptionsController {
  private final GroupManager groupManager;
  private final GroupPickerSearchService groupPickerSearchService;
  private final I18nResolver i18nResolver;
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final UserSearchService userSearchService;
  private final SearchRequestService searchRequestService;
  private final FilterSubscriptionService filterSubscriptionService;
  private final FilterSubscriptionRepository filterSubscriptionRepository;
  private final PermissionHelper permissionHelper;
  private final PluginData pluginData;

  @Autowired
  public FilterSubscriptionsController(
      @ComponentImport GroupManager groupManager,
      @ComponentImport GroupPickerSearchService groupPickerSearchService,
      @ComponentImport I18nResolver i18nResolver,
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      UserSearchService userSearchService,
      SearchRequestService searchRequestService,
      FilterSubscriptionService filterSubscriptionService,
      FilterSubscriptionRepository filterSubscriptionRepository,
      PermissionHelper permissionHelper,
      PluginData pluginData) {
    this.groupManager = groupManager;
    this.groupPickerSearchService = groupPickerSearchService;
    this.i18nResolver = i18nResolver;
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.userSearchService = userSearchService;
    this.searchRequestService = searchRequestService;
    this.filterSubscriptionService = filterSubscriptionService;
    this.filterSubscriptionRepository = filterSubscriptionRepository;
    this.permissionHelper = permissionHelper;
    this.pluginData = pluginData;
  }

  @GET
  public List<FilterSubscriptionDto> getSubscriptions(
      @QueryParam("subscribers") final List<String> subscribers,
      @QueryParam("filterId") final Long filterId,
      @QueryParam("recipientsType") final RecipientsType recipientsType,
      @QueryParam("recipients") final List<String> recipients) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    if (permissionHelper.isJiraAdmin(loggedInUser)) {
      return Arrays.stream(
              filterSubscriptionRepository.getSubscriptions(
                  subscribers, filterId, recipientsType, recipients))
          .map(filterSubscriptionRepository::entityToDto)
          .collect(Collectors.toList());
    } else {
      return Arrays.stream(
              filterSubscriptionRepository.getSubscriptions(
                  Collections.singletonList(loggedInUser.getKey()),
                  filterId,
                  recipientsType,
                  recipients))
          .map(filterSubscriptionRepository::entityToDto)
          .collect(Collectors.toList());
    }
  }

  @POST
  public void createSubscriptions(FilterSubscriptionDto filterSubscriptionDto) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }
    filterSubscriptionService.createFilterSubscription(filterSubscriptionDto);
  }

  @PUT
  @Path("{id}")
  public void updateSubscriptions(
      @PathParam("id") int id, FilterSubscriptionDto filterSubscriptionDto) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }
    FilterSubscription subscription = filterSubscriptionRepository.get(id);
    permissionHelper.checkSubscriptionPermission(loggedInUser, subscription);

    filterSubscriptionService.updateFilterSubscription(id, filterSubscriptionDto);
  }

  @DELETE
  @Path("{id}")
  public void deleteSubscription(@PathParam("id") int id) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }
    FilterSubscription subscription = filterSubscriptionRepository.get(id);
    permissionHelper.checkSubscriptionPermission(loggedInUser, subscription);

    filterSubscriptionService.deleteFilterSubscription(id);
  }

  @POST
  @Path("{id}/run")
  public void runSubscription(@PathParam("id") int id) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }
    FilterSubscription subscription = filterSubscriptionRepository.get(id);
    permissionHelper.checkSubscriptionPermission(loggedInUser, subscription);

    filterSubscriptionService.runFilterSubscription(id);
  }

  @GET
  @Path("filters")
  public List<JqlFilterDto> searchJqlFilters(@QueryParam("query") final String query) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    try {
      SharedEntitySearchParameters searchParameters =
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
    } catch (SearchParseException e) {
      throw new RestFieldException(
          i18nResolver.getText("common.sharing.exception.search.parse"), "filter");
    }
  }

  @GET
  @Path("users")
  public List<UserDto> searchUsers(@QueryParam("query") final String query) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    if (!permissionHelper.isJiraAdmin(loggedInUser))
      return Collections.singletonList(new UserDto(loggedInUser));

    UserSearchParams searchParams =
        new UserSearchParams(true, true, false, true, null, null, 10, true, false);
    return userSearchService.findUsers(query, searchParams).stream()
        .map(UserDto::new)
        .collect(Collectors.toList());
  }

  @GET
  @Path("groups")
  public List<GroupDto> searchGroups(@QueryParam("query") final String query) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

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

  @GET
  @Path("permissions")
  public FilterSubscriptionPermissionsDto getPermissions() {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }

    return new FilterSubscriptionPermissionsDto(permissionHelper.isJiraAdmin(loggedInUser));
  }

  private boolean isGroupExcluded(String group) {
    return pluginData.getSubscriptionsExcludingGroups().contains(group);
  }
}
