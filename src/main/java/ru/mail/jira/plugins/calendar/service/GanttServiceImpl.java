package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.order.SortOrder;
import com.google.common.collect.Sets;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.configuration.NonWorkingDay;
import ru.mail.jira.plugins.calendar.configuration.WorkingDaysService;
import ru.mail.jira.plugins.calendar.configuration.WorkingTimeDto;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.GanttLink;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventGroup;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class GanttServiceImpl implements GanttService {
    private final ActiveObjects ao;
    private final TimeTrackingConfiguration timeTrackingConfiguration;
    private final TimeZoneManager timeZoneManager;
    private final JiraDeprecatedService jiraDeprecatedService;
    private final PermissionService permissionService;
    private final CalendarEventService calendarEventService;
    private final CalendarService calendarService;
    private final WorkingDaysService workingDaysService;

    @Autowired
    public GanttServiceImpl(
        @ComponentImport ActiveObjects ao,
        @ComponentImport TimeTrackingConfiguration timeTrackingConfiguration,
        @ComponentImport TimeZoneManager timeZoneManager,
        JiraDeprecatedService jiraDeprecatedService,
        PermissionService permissionService,
        CalendarEventService calendarEventService,
        CalendarService calendarService,
        WorkingDaysService workingDaysService
    ) {
        this.ao = ao;
        this.timeTrackingConfiguration = timeTrackingConfiguration;
        this.timeZoneManager = timeZoneManager;
        this.jiraDeprecatedService = jiraDeprecatedService;
        this.permissionService = permissionService;
        this.calendarEventService = calendarEventService;
        this.calendarService = calendarService;
        this.workingDaysService = workingDaysService;
    }

    @Override
    public GanttDto getGantt(ApplicationUser user, int calendarId, String startDate, String endDate, String groupBy, String orderBy, SortOrder sortOrder) throws ParseException, SearchException, GetException {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!permissionService.hasUsePermission(user, calendar)) {
            throw new SecurityException("No permission");
        }

        DateTimeFormatter dateFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE);
        DateTimeFormatter dateTimeFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);

        if (StringUtils.isEmpty(startDate))
            startDate = LocalDate.now().withDayOfYear(1).format(java.time.format.DateTimeFormatter.ISO_DATE);
        if (StringUtils.isEmpty(endDate))
            endDate = LocalDate.now().plusMonths(3).format(java.time.format.DateTimeFormatter.ISO_DATE);
        ZoneId userZoneId = timeZoneManager.getTimeZoneforUser(user).toZoneId();
        ZoneId defaultZoneId = timeZoneManager.getDefaultTimezone().toZoneId();

        GanttDto ganttDto = new GanttDto();

        Order order = null;
        if (orderBy != null && sortOrder != null) {
            order = new Order(orderBy, sortOrder);
        }
        List<EventDto> eventDtoList = calendarEventService.findEvents(calendarId, groupBy, startDate, endDate, user, true, order);

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

        List<GanttTaskDto> events = new ArrayList<>();

        eventDtoList
            .stream()
            .map(eventDto -> buildEvent(eventDto, dateFormat, dateTimeFormat, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, userZoneId))
            .forEach(events::add);

        eventDtoList
            .stream()
            .map(EventDto::getGroups)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(this::buildGroup)
            .forEach(events::add);

        ganttDto.setData(events);
        GanttCollectionsDto collectionsDto = new GanttCollectionsDto();
        collectionsDto.setLinks(
            Arrays
                .stream(ao.find(GanttLink.class, Query.select().where("CALENDAR_ID = ?", calendarId)))
                .map(GanttServiceImpl::buildLinkDto)
                .collect(Collectors.toList())
        );
        ganttDto.setCollections(collectionsDto);
        return ganttDto;
    }

    @Override
    public GanttLinkDto createLink(ApplicationUser user, int calendarId, GanttLinkForm form) throws GetException {
        Calendar calendar = calendarService.getCalendar(calendarId);

        //todo different permission
        if (!permissionService.hasUsePermission(user, calendar)) {
            throw new SecurityException("No permission");
        }

        GanttLink ganttLink = ao.create(GanttLink.class);
        ganttLink.setCalendarId(calendarId);
        ganttLink.setSource(form.getSource());
        ganttLink.setTarget(form.getTarget());
        ganttLink.setType(form.getType());
        ganttLink.save();
        return buildLinkDto(ganttLink);
    }

    @Override
    public void deleteLink(ApplicationUser user, int calendarId, int linkId) throws GetException {
        GanttLink ganttLink = ao.get(GanttLink.class, linkId);

        Calendar calendar = calendarService.getCalendar(ganttLink.getCalendarId());

        //todo different permission
        if (!permissionService.hasUsePermission(user, calendar)) {
            throw new SecurityException("No permission");
        }

        ao.delete(ganttLink);
    }

    @Override
    public List<GanttTaskDto> updateDates(ApplicationUser user, int calendarId, String issueKey, String startDate, String endDate) throws Exception {
        return updateDates(user, calendarId, issueKey, startDate, endDate, false);
    }

    public List<GanttTaskDto> updateDates(ApplicationUser user, int calendarId, String issueKey, String startDate, String endDate, boolean withDependencies) throws Exception {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!permissionService.hasUsePermission(user, calendar)) {
            throw new SecurityException("No permission");
        }

        List<GanttTaskDto> result = new ArrayList<>();

        EventDto event = calendarEventService.moveEvent(
            user,
            calendarId,
            issueKey,
            startDate,
            endDate
        );

        //todo: dependencies

        DateTimeFormatter dateFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE);
        DateTimeFormatter dateTimeFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);

        ZoneId defaultZoneId = timeZoneManager.getDefaultTimezone().toZoneId();
        ZoneId userZoneId = timeZoneManager.getTimeZoneforUser(user).toZoneId();

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

        result.add(
            buildEvent(
                event,
                dateFormat, dateTimeFormat, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, userZoneId
            )
        );


        if (withDependencies) {
            DateTimeFormatter inputFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);

            String eventEnd;
            if (calendar.getEventEnd() == null) {
                //when end date is based on estimate

                Long estimate = event.getOriginalEstimateSeconds();
                Long spent = event.getTimeSpentSeconds();

                long time = 0;
                if (estimate != null) {
                    time = estimate;

                    if (spent != null && spent > estimate) {
                        time = spent;
                    }
                }

                eventEnd = inputFormat.format(
                    addWorkTimeSeconds(
                        event.isAllDay(), event.getStartDate(), time, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, userZoneId
                    )
                );
            } else {
                eventEnd = event.getEnd();
            }


            //todo: probably check for events that depend on this event
            if (eventEnd != null) {
                for (String key : findDependencies(issueKey)) {
                    //todo: if depEvent.start<eventEnd
                    result.addAll(updateDates(user, calendarId, key, eventEnd, eventEnd));
                }
            }
        }

        return result;
    }

    private List<String> findDependencies(String source) {
        return Arrays
            .stream(ao.find(GanttLink.class, Query.select().where("SOURCE = ?", source)))
            .map(GanttLink::getTarget)
            .collect(Collectors.toList());
    }

    private GanttTaskDto buildEvent(
        EventDto event, DateTimeFormatter dateFormatter, DateTimeFormatter dateTimeFormatter, long secondsPerWeek, long secondsPerDay,
        Set<Integer> workingDays, Set<java.time.LocalDate> nonWorkingDays, WorkingTimeDto workingTime, ZoneId zoneId
    ) {
        GanttTaskDto ganttTaskDto = new GanttTaskDto();
        ganttTaskDto.setType("issue");
        ganttTaskDto.setOriginalEvent(event);
        ganttTaskDto.setId(event.getId());
        ganttTaskDto.setSummary(event.getTitle());
        ganttTaskDto.setText(String.format("%s %s", event.getId(), event.getTitle()));
        ganttTaskDto.setIconSrc(event.getIssueTypeImgUrl());
        ganttTaskDto.setMovable(event.isStartEditable());
        ganttTaskDto.setResizable(event.isDurationEditable());
        ganttTaskDto.setResolved(event.isResolved());
        List<EventGroup> groups = event.getGroups();
        if (groups != null && groups.size() > 0) {
            ganttTaskDto.setParent(groups.get(0).getId());
        }

        if (event.getIssueInfo() != null) {
            if (event.getAssignee() != null) {
                ganttTaskDto.setResource(event.getAssignee().getKey());
            }
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

    private GanttTaskDto buildGroup(EventGroup group) {
        GanttTaskDto result = new GanttTaskDto();

        result.setId(group.getId());
        result.setText(group.getName());
        result.setSummary(group.getName());
        result.setIconSrc(group.getAvatar());
        result.setType("group");

        return result;
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
