/* (C)2023 */
package ru.mail.jira.plugins.myteam.controller;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.ApplicationUser;
import java.util.List;
import javax.naming.NoPermissionException;
import javax.ws.rs.*;
import org.springframework.stereotype.Controller;
import ru.mail.jira.plugins.myteam.controller.dto.ReminderDto;
import ru.mail.jira.plugins.myteam.service.ReminderService;

@Controller
@Path("/reminder")
public class ReminderController {
  private final JiraAuthenticationContext jiraAuthenticationContext;
  private final ReminderService reminderService;

  public ReminderController(
      JiraAuthenticationContext jiraAuthenticationContext, ReminderService reminderService) {
    this.jiraAuthenticationContext = jiraAuthenticationContext;
    this.reminderService = reminderService;
  }

  @POST
  @RequiresXsrfCheck
  @Path("")
  public int createReminder(final ReminderDto reminder) {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    return reminderService.addReminder(reminder, user);
  }

  @GET
  @Path("")
  public List<ReminderDto> getIssueReminders(@QueryParam("issueKey") final String issueKey) {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    return reminderService.getIssueReminders(issueKey, user);
  }

  @DELETE
  @Path("{id}")
  public void deleteReminder(@PathParam("id") final Integer id) throws NoPermissionException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    reminderService.deleteReminder(id, user);
  }

  @GET
  @Path("{id}")
  public ReminderDto getReminder(@PathParam("id") final Integer id) throws NoPermissionException {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    return reminderService.getReminder(id, user);
  }
}
