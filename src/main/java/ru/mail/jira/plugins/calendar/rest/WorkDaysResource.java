package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.datetime.DateTimeFormatUtils;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.calendar.configuration.NonWorkingDay;
import ru.mail.jira.plugins.calendar.configuration.NonWorkingDayDto;
import ru.mail.jira.plugins.calendar.configuration.WorkingDaysService;
import ru.mail.jira.plugins.calendar.configuration.WorkingTimeDto;
import ru.mail.jira.plugins.commons.RestExecutor;
import ru.mail.jira.plugins.commons.RestFieldException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Scanned
@Path("/configuration/workingDays")
@Produces(MediaType.APPLICATION_JSON)
public class WorkDaysResource {
    private final I18nHelper i18nHelper;
    private final DateTimeFormatterFactory dateTimeFormatterFactory;
    private final WorkingDaysService workingDaysService;

    public WorkDaysResource(
        @ComponentImport I18nHelper i18nHelper,
        @ComponentImport DateTimeFormatterFactory dateTimeFormatterFactory,
        WorkingDaysService workingDaysService
    ) {
        this.i18nHelper = i18nHelper;
        this.dateTimeFormatterFactory = dateTimeFormatterFactory;
        this.workingDaysService = workingDaysService;
    }

    @Path("/time")
    @GET
    public Response getWorkingTime() {
        return new RestExecutor<WorkingTimeDto>() {
            @Override
            protected WorkingTimeDto doAction() {
                return workingDaysService.getWorkingTime();
            }
        }.getResponse();
    }

    @Path("/time")
    @POST
    public Response updateWorkingTime(@FormParam("startTime") String startTime, @FormParam("endTime") String endTime) {
        //todo: permissions
        return new RestExecutor<Void>() {
            @Override
            protected Void doAction() {
                if (StringUtils.isEmpty(startTime)) {
                    throw new RestFieldException(
                        i18nHelper.getText(
                            "issue.field.required",
                            i18nHelper.getText("ru.mail.jira.plugins.calendar.configuration.workingTime.startTime")
                        ),
                        "startTime"
                    );
                }
                if (StringUtils.isEmpty(endTime)) {
                    throw new RestFieldException(
                        i18nHelper.getText(
                            "issue.field.required",
                            i18nHelper.getText("ru.mail.jira.plugins.calendar.configuration.workingTime.endTime")
                        ),
                        "endTime"
                    );
                }
                try {
                    workingDaysService.setWorkingTime(new WorkingTimeDto(
                        LocalTime.parse(startTime),
                        LocalTime.parse(endTime)
                    ));
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(
                        i18nHelper.getText("ru.mail.jira.plugins.calendar.configuration.workingTime.formatError")
                    );
                }
                return null;
            }
        }.getResponse();
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

    private String getFormattedDatePicker(Date date) {
        return (date == null) ? null : dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER).format(date);

    }

    private Date getParsedDatePicker(String date) {
        return (date == null) ? null :  dateTimeFormatterFactory.formatter().forLoggedInUser().withStyle(DateTimeStyle.DATE_PICKER).parse(date);
    }
}
