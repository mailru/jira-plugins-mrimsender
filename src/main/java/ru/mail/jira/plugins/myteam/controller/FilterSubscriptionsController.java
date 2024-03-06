/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionDto;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionPermissionsDto;
import ru.mail.jira.plugins.myteam.controller.dto.GroupDto;
import ru.mail.jira.plugins.myteam.controller.dto.JqlFilterDto;
import ru.mail.jira.plugins.myteam.controller.dto.UserDto;
import ru.mail.jira.plugins.myteam.db.model.RecipientsType;
import ru.mail.jira.plugins.myteam.service.FilterSubscriptionService;
import ru.mail.jira.plugins.myteam.service.subscription.FilterSubscriptionDataSearcher;

@Controller
@Path("/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
public class FilterSubscriptionsController {
  private final FilterSubscriptionService filterSubscriptionService;
  private final FilterSubscriptionDataSearcher filterSubscriptionDataSearcher;

  @Autowired
  public FilterSubscriptionsController(
      final FilterSubscriptionService filterSubscriptionService,
      final FilterSubscriptionDataSearcher filterSubscriptionDataSearcher) {
    this.filterSubscriptionService = filterSubscriptionService;
    this.filterSubscriptionDataSearcher = filterSubscriptionDataSearcher;
  }

  @GET
  public List<FilterSubscriptionDto> getSubscriptions(
      @QueryParam("subscribers") final List<String> subscribers,
      @QueryParam("filterId") final Long filterId,
      @QueryParam("recipientsType") final RecipientsType recipientsType,
      @QueryParam("recipients") final List<String> recipients) {
    return filterSubscriptionService.getSubscriptions(
        subscribers, filterId, recipientsType, recipients);
  }

  @POST
  public void createSubscriptions(FilterSubscriptionDto filterSubscriptionDto) {
    filterSubscriptionService.createFilterSubscription(filterSubscriptionDto);
  }

  @PUT
  @Path("{id}")
  public void updateSubscriptions(
      @PathParam("id") int id, FilterSubscriptionDto filterSubscriptionDto) {
    filterSubscriptionService.updateFilterSubscription(id, filterSubscriptionDto);
  }

  @DELETE
  @Path("{id}")
  public void deleteSubscription(@PathParam("id") int id) {
    filterSubscriptionService.deleteFilterSubscription(id);
  }

  @POST
  @Path("{id}/run")
  public void runSubscription(@PathParam("id") int id) {
    filterSubscriptionService.runFilterSubscriptionIfUserHasPermission(id);
  }

  @GET
  @Path("filters")
  public List<JqlFilterDto> searchJqlFilters(@QueryParam("query") final String query) {
    return filterSubscriptionDataSearcher.searchJqlFilters(query);
  }

  @GET
  @Path("users")
  public List<UserDto> searchUsers(@QueryParam("query") final String query) {
    return filterSubscriptionDataSearcher.searchUsers(query);
  }

  @GET
  @Path("groups")
  public List<GroupDto> searchGroups(@QueryParam("query") final String query) {
    return filterSubscriptionDataSearcher.searchGroups(query);
  }

  @GET
  @Path("permissions")
  public FilterSubscriptionPermissionsDto getPermissions() {
    return filterSubscriptionDataSearcher.getPermissions();
  }
}
