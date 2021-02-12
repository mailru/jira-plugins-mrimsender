package ru.mail.jira.plugins.calendar.configuration;

import com.atlassian.jira.datetime.DateTimeFormatUtils;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/configuration/workingDays")
@Produces(MediaType.APPLICATION_JSON)
public class MailRuCalendarWorkingDaysConfigurationAction extends JiraWebActionSupport {
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final WorkingDaysService workingDaysService;
    private final GlobalPermissionManager globalPermissionManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;

    public MailRuCalendarWorkingDaysConfigurationAction(
            @ComponentImport DateTimeFormatterFactory dateTimeFormatterFactory,
            WorkingDaysService workingDaysService,
            @ComponentImport GlobalPermissionManager globalPermissionManager,
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext) {
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.workingDaysService = workingDaysService;
        this.globalPermissionManager = globalPermissionManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
    }

    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }

    private String getFormattedDatePicker(Date date) {
        return (date == null) ? null : dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER).format(date);

    }

    private Date getParsedDatePicker(String date) {
        return (date == null) ? null : dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER).parse(date);
    }

    public List<Integer> getWorkingDays() {
        return workingDaysService.getWorkingDays();
    }

    @POST
    public Response updateWorkingDays(@FormParam("days") final String days) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                if (!isAdministrator(getJiraServiceContext().getLoggedInApplicationUser())) {
                    throw new SecurityException("User doesn't have admin permissions");
                }
                workingDaysService.setWorkingDays(days);
                return null;
            }
        }.getResponse();
    }

    @GET
    @Path("nonWorkingDays")
    public Response getNonWorkingDays() {
        return new RestExecutor<List<NonWorkingDayDto>>() {
            @Override
            protected List<NonWorkingDayDto> doAction() throws Exception {
                List<NonWorkingDayDto> result = new ArrayList<>();
                for (NonWorkingDay nonWorkingDay : workingDaysService.getNonWorkingDays())
                    result.add(new NonWorkingDayDto(nonWorkingDay.getID(), getFormattedDatePicker(nonWorkingDay.getDate()), nonWorkingDay.getDescription()));
                return result;
            }
        }.getResponse();
    }


    @POST
    @Path("nonWorkingDay")
    public Response createNonWorkingDay(final NonWorkingDayDto nonWorkingDayDto) {
        return new RestExecutor<NonWorkingDayDto>() {
            @Override
            protected NonWorkingDayDto doAction() throws Exception {
                if (!isAdministrator(getJiraServiceContext().getLoggedInApplicationUser())) {
                    throw new SecurityException("User doesn't have admin permissions");
                }
                NonWorkingDay nonWorkingDay = workingDaysService.addNonWorkingDay(getParsedDatePicker(nonWorkingDayDto.getDate()), nonWorkingDayDto.getDescription());
                return new NonWorkingDayDto(nonWorkingDay.getID(), getFormattedDatePicker(nonWorkingDay.getDate()), nonWorkingDay.getDescription());
            }
        }.getResponse();
    }

    @DELETE
    @Path("nonWorkingDay/{id}")
    public Response createNonWorkingDay(@PathParam("id") final int id) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
                if (!isAdministrator(getJiraServiceContext().getLoggedInApplicationUser())) {
                    throw new SecurityException("User doesn't have admin permissions");
                }
                workingDaysService.deleteNonWorkingDay(id);
                return null;
            }
        }.getResponse();
    }

    @GET
    @Path("/calendar/params")
    public Response getCalendarParams() {
        return new RestExecutor<Map<String, String>>() {
            @Override
            protected Map<String, String> doAction() {
                Map<String, String> result = new HashMap<String, String>();
                result.put("dateTimeFormat", DateTimeFormatUtils.getDateTimeFormat());
                result.put("dateFormat", DateTimeFormatUtils.getDateFormat());
                result.put("timeFormat", "%H:%M");
                return result;
            }
        }.getResponse();
    }

    private boolean isAdministrator(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }
}
