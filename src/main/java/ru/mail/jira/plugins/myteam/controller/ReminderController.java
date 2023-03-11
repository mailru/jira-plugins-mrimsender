/* (C)2023 */
package ru.mail.jira.plugins.myteam.controller;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.ApplicationUser;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
  public int createChatSettings(final ReminderDto reminder) {
    ApplicationUser user = jiraAuthenticationContext.getLoggedInUser();
    return reminderService.addReminder(reminder, user);
  }
}
