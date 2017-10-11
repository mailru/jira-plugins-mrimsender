package ru.mail.jira.plugins.calendar.configuration;

import com.atlassian.jira.datetime.DateTimeFormatUtils;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import electric.soap.rpc.In;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Scanned
@Path("/configuration/workingDays")
@Produces(MediaType.APPLICATION_JSON)
public class MailRuCalendarWorkingDaysConfigurationAction extends JiraWebActionSupport {
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final WorkingDaysService workingDaysService;

    public MailRuCalendarWorkingDaysConfigurationAction(
            @ComponentImport DateTimeFormatterFactory dateTimeFormatterFactory,
            WorkingDaysService workingDaysService
    ) {
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.workingDaysService = workingDaysService;
    }

    @Override
    public String execute() throws Exception {
        return SUCCESS;
    }

    private String getFormattedDatePicker(Date date) {
        return (date == null) ? null : dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER).format(date);

    }

    private Date getParsedDatePicker(String date) {
        return (date == null) ? null :  dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER).parse(date);
    }

    public List<Integer> getWorkingDays() {
        return workingDaysService.getWorkingDays();
    }

    @POST
    public Response updateWorkingDays(@FormParam("days") final String days) {
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() throws Exception {
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
}
