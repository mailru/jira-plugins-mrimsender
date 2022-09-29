/* (C)2022 */
package ru.mail.jira.plugins.myteam.controller;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.component.PermissionHelper;
import ru.mail.jira.plugins.myteam.controller.dto.FilterSubscriptionDto;
import ru.mail.jira.plugins.myteam.db.model.FilterSubscription;
import ru.mail.jira.plugins.myteam.db.repository.FilterSubscriptionRepository;

@Controller
@Path("/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
public class FilterSubscriptionsController {
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final FilterSubscriptionRepository filterSubscriptionRepository;
  private final PermissionHelper permissionHelper;

  @Autowired
  public FilterSubscriptionsController(
      @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
      FilterSubscriptionRepository filterSubscriptionRepository,
      PermissionHelper permissionHelper) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
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
}
