package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Sets;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.configuration.NonWorkingDay;
import ru.mail.jira.plugins.calendar.configuration.WorkingDaysService;
import ru.mail.jira.plugins.calendar.configuration.WorkingTimeDto;
import ru.mail.jira.plugins.calendar.model.GanttLink;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class GanttServiceImpl implements GanttService {
    private final ActiveObjects ao;
    private final JiraAuthenticationContext authenticationContext;
    private final TimeTrackingConfiguration timeTrackingConfiguration;
    private final TimeZoneManager timeZoneManager;
    private final JiraDeprecatedService jiraDeprecatedService;
    private final CalendarEventService calendarEventService;
    private final WorkingDaysService workingDaysService;

    @Autowired
    public GanttServiceImpl(
        @ComponentImport ActiveObjects ao,
        @ComponentImport JiraAuthenticationContext authenticationContext,
        @ComponentImport TimeTrackingConfiguration timeTrackingConfiguration,
        @ComponentImport TimeZoneManager timeZoneManager,
        JiraDeprecatedService jiraDeprecatedService,
        CalendarEventService calendarEventService,
        WorkingDaysService workingDaysService
    ) {
        this.ao = ao;
        this.authenticationContext = authenticationContext;
        this.timeTrackingConfiguration = timeTrackingConfiguration;
        this.timeZoneManager = timeZoneManager;
        this.jiraDeprecatedService = jiraDeprecatedService;
        this.calendarEventService = calendarEventService;
        this.workingDaysService = workingDaysService;
    }

    @Override
    public GanttDto getGantt(int calendarId, String startDate, String endDate) throws ParseException, SearchException, GetException {
        //todo: permissions
        DateTimeFormatter dateFormat = jiraDeprecatedService.dateTimeFormatter.forUser(authenticationContext.getLoggedInUser()).withStyle(DateTimeStyle.ISO_8601_DATE);
        DateTimeFormatter dateTimeFormat = jiraDeprecatedService.dateTimeFormatter.forUser(authenticationContext.getLoggedInUser()).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);

        if (StringUtils.isEmpty(startDate))
            startDate = LocalDate.now().withDayOfYear(1).toString("yyyy-MM-dd");
        if (StringUtils.isEmpty(endDate))
            endDate = LocalDate.now().plusMonths(3).toString("yyyy-MM-dd");
        ZoneId userZoneId = timeZoneManager.getTimeZoneforUser(authenticationContext.getLoggedInUser()).toZoneId();
        ZoneId defaultZoneId = timeZoneManager.getDefaultTimezone().toZoneId();

        GanttDto ganttDto = new GanttDto();
        List<EventDto> eventDtoList = calendarEventService.findEvents(calendarId, null, startDate, endDate, authenticationContext.getLoggedInUser(), true);

        WorkingTimeDto workingTime = workingDaysService.getWorkingTime();
        Set<Integer> workingDays = Sets.newHashSet(workingDaysService.getWorkingDays());

        long secondsPerDay = workingTime.getEndTime().toSecondOfDay() - workingTime.getStartTime().toSecondOfDay();
        long secondsPerWeek = workingDays.size() * secondsPerDay;

        Set<java.time.LocalDate> nonWorkingDays = Arrays
            .stream(workingDaysService.getNonWorkingDays())
            .map(NonWorkingDay::getDate)
            .map(date -> date.toInstant().atZone(defaultZoneId))
            .map(ZonedDateTime::toLocalDate)
            .collect(Collectors.toSet());

        ganttDto.setData(
            eventDtoList
                .stream()
                .map(eventDto -> buildEvent(eventDto, dateFormat, dateTimeFormat, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, userZoneId))
                .toArray(GanttTaskDto[]::new)
        );
        GanttCollectionsDto collectionsDto = new GanttCollectionsDto();
        collectionsDto.setLinks(
            Arrays
                .stream(ao.find(GanttLink.class, Query.select().where("CALENDAR_ID = ?", calendarId)))
                .map(GanttServiceImpl::buildLinkDto)
                .toArray(GanttLinkDto[]::new)
        );
        ganttDto.setCollections(collectionsDto);
        return ganttDto;
    }

    @Override
    public GanttLinkDto createLink(int calendarId, GanttLinkForm form) {
        //todo: permissions
        GanttLink ganttLink = ao.create(GanttLink.class);
        ganttLink.setCalendarId(calendarId);
        ganttLink.setSource(form.getSource());
        ganttLink.setTarget(form.getTarget());
        ganttLink.setType(form.getType());
        ganttLink.save();
        return buildLinkDto(ganttLink);
    }

    @Override
    public void deleteLink(int calendarId, int linkId) {
        //todo: permissions
        GanttLink ganttLink = ao.get(GanttLink.class, linkId);
        ao.delete(ganttLink);
    }

    @Override
    public GanttTaskDto updateDates(int calendarId, String issueKey, String startDate, String endDate) throws Exception {
        //todo: permissions
        EventDto event = calendarEventService.moveEvent(
            authenticationContext.getLoggedInUser(),
            calendarId,
            issueKey,
            startDate,
            endDate
        );

        DateTimeFormatter dateFormat = jiraDeprecatedService.dateTimeFormatter.forUser(authenticationContext.getLoggedInUser()).withStyle(DateTimeStyle.ISO_8601_DATE);
        DateTimeFormatter dateTimeFormat = jiraDeprecatedService.dateTimeFormatter.forUser(authenticationContext.getLoggedInUser()).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);

        ZoneId defaultZoneId = timeZoneManager.getDefaultTimezone().toZoneId();
        ZoneId userZoneId = timeZoneManager.getTimeZoneforUser(authenticationContext.getLoggedInUser()).toZoneId();

        BigDecimal secondsPerHour = BigDecimal.valueOf(com.atlassian.core.util.DateUtils.Duration.HOUR.getSeconds());
        long secondsPerDay = timeTrackingConfiguration.getHoursPerDay().multiply(secondsPerHour).longValueExact();
        long secondsPerWeek = timeTrackingConfiguration.getDaysPerWeek().multiply(timeTrackingConfiguration.getHoursPerDay()).multiply(secondsPerHour).longValueExact();

        Set<Integer> workingDays = Sets.newHashSet(workingDaysService.getWorkingDays());
        WorkingTimeDto workingTime = workingDaysService.getWorkingTime();

        Set<java.time.LocalDate> nonWorkingDays = Arrays
            .stream(workingDaysService.getNonWorkingDays())
            .map(NonWorkingDay::getDate)
            .map(date -> date.toInstant().atZone(defaultZoneId))
            .map(ZonedDateTime::toLocalDate)
            .collect(Collectors.toSet());

        return buildEvent(
            event,
            dateFormat, dateTimeFormat, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, userZoneId
        );
    }

    private GanttTaskDto buildEvent(
        EventDto event, DateTimeFormatter dateFormatter, DateTimeFormatter dateTimeFormatter, long secondsPerWeek, long secondsPerDay,
        Set<Integer> workingDays, Set<java.time.LocalDate> nonWorkingDays, WorkingTimeDto workingTime, ZoneId zoneId
    ) {
        GanttTaskDto ganttTaskDto = new GanttTaskDto();
        ganttTaskDto.setId(event.getId());
        ganttTaskDto.setSummary(event.getTitle());
        ganttTaskDto.setText(String.format("%s %s", event.getId(), event.getTitle()));
        ganttTaskDto.setIconSrc(event.getIssueTypeImgUrl());
        ganttTaskDto.setMovable(event.isStartEditable());
        ganttTaskDto.setResizable(event.isDurationEditable());
        ganttTaskDto.setResolved(event.isResolved());

        if (event.getIssueInfo() != null) {
            ganttTaskDto.setAssignee(event.getIssueInfo().getAssignee());
        }

        DateTimeFormatter suitableFormatter = event.isAllDay() ? dateFormatter : dateTimeFormatter;

        ganttTaskDto.setStartDate(event.getStart());

        Date eventStart = event.getStartDate();
        Long originalEstimate = event.getOriginalEstimateSeconds();
        Long timeSpent = event.getTimeSpentSeconds();
        if (StringUtils.isNotEmpty(event.getEnd())) {
            ganttTaskDto.setEndDate(event.getEnd());
        } else if (originalEstimate != null) {
            Date plannedEnd = addWorkTimeSeconds(
                event.isAllDay(), eventStart, originalEstimate, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, zoneId
            );
            ganttTaskDto.setEndDate(suitableFormatter.format(plannedEnd));

            if (timeSpent != null) {
                if (timeSpent > originalEstimate) {
                    Date overdueDate = addWorkTimeSeconds(
                        event.isAllDay(), eventStart, timeSpent, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, zoneId
                    );
                    ganttTaskDto.setEndDate(suitableFormatter.format(overdueDate));

                    ganttTaskDto.setOverdueSeconds(
                        TimeUnit.MILLISECONDS.toSeconds(overdueDate.getTime() - plannedEnd.getTime())
                    );
                }

                if (event.isResolved() && timeSpent < originalEstimate) {
                    Date earlyDate = addWorkTimeSeconds(
                        event.isAllDay(), eventStart, timeSpent, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, zoneId
                    );

                    ganttTaskDto.setEarlySeconds(
                        TimeUnit.MILLISECONDS.toSeconds(plannedEnd.getTime() - earlyDate.getTime())
                    );
                }
            }
        } else {
            ganttTaskDto.setEndDate(suitableFormatter.format(new Date(eventStart.getTime() + TimeUnit.DAYS.toMillis(1))));
        }

        if (originalEstimate != null && timeSpent != null)
            ganttTaskDto.setProgress(timeSpent * 1.0 / originalEstimate);
        if (event.getOriginalEstimate() != null) {
            ganttTaskDto.setEstimate(event.getOriginalEstimate());
        }

        return ganttTaskDto;
    }

    private Date addWorkTimeSeconds(
        boolean allDay, Date sourceDate, long seconds, long secondsPerWeek, long secondsPerDay,
        Set<Integer> workingDays, Set<java.time.LocalDate> nonWorkingDays, WorkingTimeDto workingTime, ZoneId zoneId
    ) {
        boolean reduceIfWorkDay = false;

        long weeks = seconds / secondsPerWeek;
        seconds = seconds % secondsPerWeek;

        long days = seconds / secondsPerDay;
        seconds = seconds % secondsPerDay;

        ZonedDateTime date = sourceDate
            .toInstant()
            .atZone(zoneId)
            .plusWeeks(weeks);

        LocalTime startTime = workingTime.getStartTime();
        LocalTime endTime = workingTime.getEndTime();

        java.time.LocalDate localDate = date.toLocalDate();
        if (allDay) {
            if (seconds > 0) {
                days++;
            }
        } else {
            LocalTime time = date.toLocalTime();

            if (seconds <= 0) {
                if (days > 0) {
                    reduceIfWorkDay = true;
                    seconds = secondsPerDay;
                }
            }
            if (seconds > 0) {
                if (time.isBefore(startTime)) {
                    date = ZonedDateTime.of(
                        localDate,
                        startTime,
                        zoneId
                    );
                    time = startTime;
                }

                if (time.isBefore(endTime)) {
                    long secondOfDay = time.toSecondOfDay() + seconds;
                    int endSecond = endTime.toSecondOfDay();
                    seconds = secondOfDay - endSecond;
                    //if (time + seconds) > end_time: seconds=(time+seconds)-end_time, time=end_time
                    if (seconds > 0) {
                        time = endTime;
                    } else {
                        time = LocalTime.ofSecondOfDay(secondOfDay);
                    }
                    date = ZonedDateTime.of(localDate, time, zoneId);
                }

                //if after end of day: date=date+1d, time=start_time+seconds
                if (seconds > 0) {
                    days++;
                    date = ZonedDateTime.of(
                        localDate,
                        startTime.plusSeconds(seconds),
                        zoneId
                    );
                }
            }
        }

        while (days > 0) {
            if (!allDay) {
                if (reduceIfWorkDay) {
                    boolean isWeekDay = workingDays.contains(date.getDayOfWeek().getValue());
                    boolean isNonWorkingDay = nonWorkingDays.contains(date.toLocalDate());
                    boolean isWorkDay = isWeekDay && !isNonWorkingDay;

                    if (isWorkDay) {
                        days--;
                    }
                    reduceIfWorkDay = false;

                    if (days == 0) {
                        //date = date.plusDays(1);
                        continue;
                    }
                }

                date = date.plusDays(1);
            }

            boolean isWeekDay = workingDays.contains(date.getDayOfWeek().getValue());
            boolean isNonWorkingDay = nonWorkingDays.contains(date.toLocalDate());
            boolean isWorkDay = isWeekDay && !isNonWorkingDay;

            if (isWorkDay) {
                days--;
            }

            if (allDay) {
                date = date.plusDays(1);
            }
        }

        return Date.from(date.toInstant());
    }

    private static GanttLinkDto buildLinkDto(GanttLink link) {
        GanttLinkDto dto = new GanttLinkDto();
        dto.setId(link.getID());
        dto.setSource(link.getSource());
        dto.setTarget(link.getTarget());
        dto.setType(link.getType());
        dto.setColor("#505F79"); //@ak-color-N400
        return dto;
    }
}
