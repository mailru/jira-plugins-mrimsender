/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller;

import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.sharing.SharedEntityColumn;
import com.atlassian.jira.sharing.search.SharedEntitySearchContext;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Arrays;
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
import ru.mail.jira.plugins.myteam.component.PermissionHelper;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionDto;
import ru.mail.jira.plugins.myteam.controller.dto.JqlFilterDto;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;
import ru.mail.jira.plugins.myteam.db.repository.FilterSubscriptionRepository;
import ru.mail.jira.plugins.myteam.service.FilterSubscriptionService;

@Controller
@Path("/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
public class FilterSubscriptionsController {
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final SearchRequestService searchRequestService;
  private final FilterSubscriptionService filterSubscriptionService;
  private final FilterSubscriptionRepository filterSubscriptionRepository;
  private final PermissionHelper permissionHelper;

  @Autowired
  public FilterSubscriptionsController(
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      SearchRequestService searchRequestService,
      FilterSubscriptionService filterSubscriptionService,
      FilterSubscriptionRepository filterSubscriptionRepository,
      PermissionHelper permissionHelper) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.searchRequestService = searchRequestService;
    this.filterSubscriptionService = filterSubscriptionService;
    this.filterSubscriptionRepository = filterSubscriptionRepository;
    this.permissionHelper = permissionHelper;
  }

  @GET
  public List<FilterSubscriptionDto> getCurrentUserSubscriptions() {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }
    return Arrays.stream(filterSubscriptionRepository.getSubscription(loggedInUser.getKey()))
        .map(filterSubscriptionRepository::entityToDto)
        .collect(Collectors.toList());
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

    filterSubscriptionRepository.delete(subscription);
  }

  @GET
  @Path("filters")
  public List<JqlFilterDto> searchJqlFilters(@QueryParam("query") final String query) {
    ApplicationUser loggedInUser = jiraAuthenticationContext.getLoggedInUser();
    if (loggedInUser == null) {
      throw new SecurityException();
    }
    SharedEntitySearchParametersBuilder builder =
        new SharedEntitySearchParametersBuilder()
            .setName(StringUtils.isBlank(query) ? StringUtils.EMPTY : query)
            .setTextSearchMode(SharedEntitySearchParameters.TextSearchMode.WILDCARD)
            .setEntitySearchContext(SharedEntitySearchContext.USE)
            .setSortColumn(SharedEntityColumn.OWNER, true);
    return searchRequestService
        .search(new JiraServiceContextImpl(loggedInUser), builder.toSearchParameters(), 0, 100)
        .getResults()
        .stream()
        .map(searchRequest -> new JqlFilterDto(searchRequest, loggedInUser))
        .collect(Collectors.toList());
  }
}
