package ru.mail.jira.plugins.calendar.service.gantt;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.worklog.TimeTrackingConfiguration;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.customfields.impl.CalculatedCFType;
import com.atlassian.jira.issue.fields.*;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.*;
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
import ru.mail.jira.plugins.calendar.rest.dto.IssueInfo;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.*;
import ru.mail.jira.plugins.calendar.rest.dto.plan.GanttPlanForm;
import ru.mail.jira.plugins.calendar.rest.dto.plan.GanttPlanItem;
import ru.mail.jira.plugins.calendar.service.*;
import ru.mail.jira.plugins.calendar.util.DateUtil;

import java.math.BigDecimal;
import java.text.ParseException;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GanttServiceImpl implements GanttService {
    private static final Set<String> MULTI_GROUP_TYPES = ImmutableSet.of(
        "component",
        "fixVersion",
        "affectsVersion",
        "labels"
    );

    private final ActiveObjects ao;
    private final TimeTrackingConfiguration timeTrackingConfiguration;
    private final TimeZoneManager timeZoneManager;
    private final FieldManager fieldManager;
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
        @ComponentImport FieldManager fieldManager,
        JiraDeprecatedService jiraDeprecatedService,
        PermissionService permissionService,
        CalendarEventService calendarEventService,
        CalendarService calendarService,
        WorkingDaysService workingDaysService
    ) {
        this.ao = ao;
        this.timeTrackingConfiguration = timeTrackingConfiguration;
        this.timeZoneManager = timeZoneManager;
        this.fieldManager = fieldManager;
        this.jiraDeprecatedService = jiraDeprecatedService;
        this.permissionService = permissionService;
        this.calendarEventService = calendarEventService;
        this.calendarService = calendarService;
        this.workingDaysService = workingDaysService;
    }

    @Override
    public GanttDto getGantt(ApplicationUser user, int calendarId, String startDate, String endDate, GanttParams params) throws ParseException, SearchException, GetException {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!permissionService.hasUsePermission(user, calendar) && !permissionService.hasAdminPermission(user, calendar)) {
            throw new SecurityException("No permission");
        }

        if (StringUtils.isEmpty(startDate))
            startDate = LocalDate.now().withDayOfYear(1).format(java.time.format.DateTimeFormatter.ISO_DATE);
        if (StringUtils.isEmpty(endDate))
            endDate = LocalDate.now().plusMonths(3).format(java.time.format.DateTimeFormatter.ISO_DATE);

        List<EventDto> eventDtoList;
        if (params.isIncludeUnscheduled()) {
            eventDtoList = calendarEventService.getUnboundedEvents(calendar, params.getGroupBy(), user, true, params.getOrder(), params.getSprintId(), params.getFields(), params.isForPlan());
        } else {
            eventDtoList = calendarEventService.findEvents(calendarId, params.getGroupBy(), startDate, endDate, user, true, params.getOrder(), params.getFields());
        }

        return getGantt(eventDtoList, user, calendarId, params.getGroupBy());
    }

    @Override
    public GanttDto getGantt(ApplicationUser user, int calendarId, GanttParams params) throws Exception {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!permissionService.hasUsePermission(user, calendar) && !permissionService.hasAdminPermission(user, calendar)) {
            throw new SecurityException("No permission");
        }

        return getGantt(calendarEventService.getUnboundedEvents(calendar, params.getGroupBy(), user, true, params.getOrder(), params.getSprintId(), params.getFields(), params.isForPlan()), user, calendarId, params.getGroupBy());
    }

    private GanttDto getGantt(List<EventDto> eventDtoList, ApplicationUser user, int calendarId, String groupBy) {
        DateTimeFormatter dateFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE);
        DateTimeFormatter dateTimeFormat = jiraDeprecatedService.dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);

        ZoneId userZoneId = timeZoneManager.getTimeZoneforUser(user).toZoneId();
        ZoneId defaultZoneId = timeZoneManager.getDefaultTimezone().toZoneId();

        GanttDto ganttDto = new GanttDto();

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

        boolean shouldAppendGroupToId = MULTI_GROUP_TYPES.contains(groupBy);

        eventDtoList
            .stream()
            .filter(e -> e.getType() == EventDto.Type.ISSUE)
            .flatMap(eventDto -> buildEvenWithGroups(
                eventDto, dateFormat, dateTimeFormat, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, userZoneId, shouldAppendGroupToId
            ))
            .forEach(events::add);

        eventDtoList
            .stream()
            .map(EventDto::getGroups)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .distinct()
            .map(this::buildGroup)
            .forEach(events::add);

        ganttDto.setData(events);
        GanttCollectionsDto collectionsDto = new GanttCollectionsDto();
        List<GanttLinkDto> links = Arrays
            .stream(ao.find(GanttLink.class, Query.select().where("CALENDAR_ID = ?", calendarId)))
            .map(GanttServiceImpl::buildLinkDto)
            .collect(Collectors.toList());

        if (shouldAppendGroupToId) {
            Multimap<String, String> groupedTasks = HashMultimap.create();

            for (GanttTaskDto task : events) {
                if (task.getParent() != null && !task.getId().equals(task.getEntityId())) {
                    groupedTasks.put(task.getEntityId(), task.getId());
                }
            }

            if (groupedTasks.size() > 0) {
                Set<Integer> processedLinks = new HashSet<>();
                Multimap<String, GanttLinkDto> linksBySource = HashMultimap.create();
                Multimap<String, GanttLinkDto> linksByTarget = HashMultimap.create();

                for (GanttLinkDto link : links) {
                    linksBySource.put(link.getSource(), link);
                    linksByTarget.put(link.getTarget(), link);
                }

                List<GanttLinkDto> newLinks = new ArrayList<>();

                int generatedId = -1;
                for (String key : groupedTasks.keys()) {
                    for (GanttLinkDto link : linksBySource.get(key)) {
                        if (!processedLinks.contains(link.getId())) {
                            processedLinks.add(link.getId());

                            Collection<String> sources = groupedTasks.get(key);
                            Collection<String> targets;
                            if (groupedTasks.containsKey(link.getTarget())) {
                                targets = groupedTasks.get(link.getTarget());
                            } else {
                                targets = ImmutableList.of(link.getTarget());
                            }

                            generatedId = collectLinks(sources, targets, link, generatedId, newLinks);
                        }
                    }
                    for (GanttLinkDto link : linksByTarget.get(key)) {
                        if (!processedLinks.contains(link.getId())) {
                            processedLinks.add(link.getId());

                            Collection<String> targets = groupedTasks.get(key);
                            Collection<String> sources;
                            if (groupedTasks.containsKey(link.getSource())) {
                                sources = groupedTasks.get(link.getSource());
                            } else {
                                sources = ImmutableList.of(link.getSource());
                            }

                            generatedId = collectLinks(sources, targets, link, generatedId, newLinks);
                        }
                    }
                }

                links.addAll(newLinks);
            }
        }

        collectionsDto.setLinks(links);
        ganttDto.setCollections(collectionsDto);
        return ganttDto;
    }

    private int collectLinks(Collection<String> sources, Collection<String> targets, GanttLinkDto link, int id, List<GanttLinkDto> collection) {
        for (String source : sources) {
            for (String target : targets) {
                GanttLinkDto dto = new GanttLinkDto();
                dto.setId(id--);
                dto.setEntityId(link.getId());
                dto.setSource(source);
                dto.setTarget(target);
                dto.setType(link.getType());
                dto.setColor(link.getColor());
                collection.add(dto);
            }
        }
        return id;
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
    public boolean hasLinks(String issueKey) {
        return ao.count(GanttLink.class, Query.select().where("SOURCE = ? OR TARGET = ?", issueKey, issueKey)) > 0;
    }

    @Override
    public List<GanttLinkDto> getLinks(String issueKey) {
        return Arrays
            .stream(ao.find(GanttLink.class, Query.select().where("SOURCE = ? OR TARGET = ?", issueKey, issueKey)))
            .map(GanttServiceImpl::buildLinkDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<GanttTaskDto> updateDates(ApplicationUser user, int calendarId, String issueKey, String startDate, String endDate) throws Exception {
        return updateDates(user, calendarId, issueKey, startDate, endDate, false);
    }

    @Override
    public void applyPlan(ApplicationUser user, int calendarId, GanttPlanForm form) throws Exception {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!permissionService.hasUsePermission(user, calendar)) {
            throw new SecurityException("No permission");
        }

        if (!(isMutableField(calendar.getEventStart()) && isMutableField(calendar.getEventEnd()))) {
            throw new IllegalArgumentException("Calendar start and end fields must be present and mutable");
        }

        for (GanttPlanItem item : form.getItems()) {
            calendarEventService.moveEvent(
                user,
                calendarId,
                item.getTaskId(),
                item.getStartDate(),
                item.getEndDate()
            );
        }
    }

    private boolean isMutableField(String fieldId) {
        if (fieldId == null) {
            return false;
        }

        if (CalendarEventService.DUE_DATE_KEY.equals(fieldId)) {
            fieldId = "duedate";
        } else if (CalendarEventService.RESOLVED_DATE_KEY.equals(fieldId)) {
            fieldId = "resolutiondate";
        }

        Field field = fieldManager.getField(fieldId);

        return field != null && !(
            field instanceof CalculatedCFType || field instanceof CreatedSystemField ||
            field instanceof UpdatedSystemField || field instanceof ResolutionDateSystemField
        );
    }

    //todo: group by parameter
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
                dateFormat, dateTimeFormat, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, userZoneId, null
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
                    DateUtil.addWorkTimeSeconds(
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

    private Stream<GanttTaskDto> buildEvenWithGroups(
        EventDto event, DateTimeFormatter dateFormatter, DateTimeFormatter dateTimeFormatter, long secondsPerWeek, long secondsPerDay,
        Set<Integer> workingDays, Set<java.time.LocalDate> nonWorkingDays, WorkingTimeDto workingTime, ZoneId zoneId, boolean addGroupId
    ) {
        if (addGroupId) {
            if (event.getGroups() != null && event.getGroups().size() > 0) {
                return event
                    .getGroups()
                    .stream()
                    .map(group -> buildEvent(
                        event, dateFormatter, dateTimeFormatter, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, zoneId, group.getId()
                    ));
            }
        }
        return Stream.of(buildEvent(
                event, dateFormatter, dateTimeFormatter, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, zoneId, null
            ));
    }

    private GanttTaskDto buildEvent(
        EventDto event, DateTimeFormatter dateFormatter, DateTimeFormatter dateTimeFormatter, long secondsPerWeek, long secondsPerDay,
        Set<Integer> workingDays, Set<java.time.LocalDate> nonWorkingDays, WorkingTimeDto workingTime, ZoneId zoneId, String groupId
    ) {
        GanttTaskDto ganttTaskDto = new GanttTaskDto();
        ganttTaskDto.setType("issue");
        ganttTaskDto.setOriginalEvent(event);

        List<EventGroup> groups = event.getGroups();
        if (groupId != null) {
            ganttTaskDto.setId(groupId + '/' + event.getId());
            ganttTaskDto.setParent(groupId);
            ganttTaskDto.setMovable(false);
            ganttTaskDto.setResizable(false);
            ganttTaskDto.setLinkable(false);
        } else {
            ganttTaskDto.setId(event.getId());
            if (groups != null && groups.size() == 1) {
                ganttTaskDto.setParent(groups.get(0).getId());
            }
            ganttTaskDto.setLinkable(true);
            ganttTaskDto.setMovable(event.isStartEditable());
            ganttTaskDto.setResizable(event.isDurationEditable());
        }
        ganttTaskDto.setEntityId(event.getId());
        ganttTaskDto.setSummary(event.getTitle());
        ganttTaskDto.setText(String.format("%s %s", event.getId(), event.getTitle()));
        ganttTaskDto.setIconSrc(event.getIssueTypeImgUrl());
        ganttTaskDto.setResolved(event.isResolved());

        IssueInfo issueInfo = event.getIssueInfo();
        if (issueInfo != null) {
            if (event.getAssignee() != null) {
                ganttTaskDto.setResource(event.getAssignee().getKey());
            }
            ganttTaskDto.setAssignee(issueInfo.getAssignee());

            if (issueInfo.getCustomFields() != null) {
                ganttTaskDto.setFields(issueInfo.getCustomFields());
            }
        }

        DateTimeFormatter suitableFormatter = event.isAllDay() ? dateFormatter : dateTimeFormatter;

        ganttTaskDto.setStartDate(event.getStart());

        Date eventStart = event.getStartDate();
        Long originalEstimate = event.getOriginalEstimateSeconds();
        Long timeSpent = event.getTimeSpentSeconds();
        if (eventStart == null) {
            ganttTaskDto.setUnscheduled(true);
        }

        if (StringUtils.isNotEmpty(event.getEnd())) {
            ganttTaskDto.setEndDate(event.getEnd());
        } else if (eventStart != null) {
            if (originalEstimate != null) {
                Date plannedEnd = DateUtil.addWorkTimeSeconds(
                    event.isAllDay(), eventStart, originalEstimate, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, zoneId
                );
                ganttTaskDto.setEndDate(suitableFormatter.format(plannedEnd));

                if (timeSpent != null) {
                    if (timeSpent > originalEstimate) {
                        Date overdueDate = DateUtil.addWorkTimeSeconds(
                            event.isAllDay(), eventStart, timeSpent, secondsPerWeek, secondsPerDay, workingDays, nonWorkingDays, workingTime, zoneId
                        );
                        ganttTaskDto.setEndDate(suitableFormatter.format(overdueDate));

                        ganttTaskDto.setOverdueSeconds(
                            TimeUnit.MILLISECONDS.toSeconds(overdueDate.getTime() - plannedEnd.getTime())
                        );
                    }

                    if (event.isResolved() && timeSpent < originalEstimate) {
                        Date earlyDate = DateUtil.addWorkTimeSeconds(
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
        result.setOpen(true);

        return result;
    }

    private static GanttLinkDto buildLinkDto(GanttLink link) {
        GanttLinkDto dto = new GanttLinkDto();
        dto.setId(link.getID());
        dto.setCalendarId(link.getCalendarId());
        dto.setSource(link.getSource());
        dto.setTarget(link.getTarget());
        dto.setType(link.getType());
        dto.setColor("#505F79"); //@ak-color-N400
        return dto;
    }
}
