package ru.mail.jira.plugins.calendar.service.gantt;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.customfields.impl.CalculatedCFType;
import com.atlassian.jira.issue.fields.CreatedSystemField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.ResolutionDateSystemField;
import com.atlassian.jira.issue.fields.UpdatedSystemField;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.configuration.NonWorkingDay;
import ru.mail.jira.plugins.calendar.configuration.WorkingDaysService;
import ru.mail.jira.plugins.calendar.configuration.WorkingTimeDto;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.GanttLink;
import ru.mail.jira.plugins.calendar.model.UserCalendar;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventGroup;
import ru.mail.jira.plugins.calendar.rest.dto.EventMilestoneDto;
import ru.mail.jira.plugins.calendar.rest.dto.IssueInfo;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttCollectionsDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttEstimateForm;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttLinkDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttLinkForm;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttResourceDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttTaskDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttTaskForm;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttTeamDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttUserDto;
import ru.mail.jira.plugins.calendar.rest.dto.plan.GanttPlanForm;
import ru.mail.jira.plugins.calendar.rest.dto.plan.GanttPlanItem;
import ru.mail.jira.plugins.calendar.service.CalendarEventService;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.calendar.service.PermissionService;
import ru.mail.jira.plugins.calendar.service.UserCalendarService;
import ru.mail.jira.plugins.calendar.util.DateUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
    private final TimeZoneManager timeZoneManager;
    private final FieldManager fieldManager;
    private final I18nHelper i18nHelper;
    private final PermissionService permissionService;
    private final CalendarEventService calendarEventService;
    private final CalendarService calendarService;
    private final WorkingDaysService workingDaysService;
    private final GanttTeamService ganttTeamService;
    private final UserManager userManager;
    private final UserCalendarService userCalendarService;

    @Autowired
    public GanttServiceImpl(
            @ComponentImport ActiveObjects ao,
            @ComponentImport TimeZoneManager timeZoneManager,
            @ComponentImport FieldManager fieldManager,
            @ComponentImport UserManager userManager,
            @ComponentImport I18nHelper i18nHelper,
            PermissionService permissionService,
            CalendarEventService calendarEventService,
            CalendarService calendarService,
            WorkingDaysService workingDaysService,
            GanttTeamService ganttTeamService,
            UserCalendarService userCalendarService) {
        this.ao = ao;
        this.timeZoneManager = timeZoneManager;
        this.fieldManager = fieldManager;
        this.i18nHelper = i18nHelper;
        this.permissionService = permissionService;
        this.calendarEventService = calendarEventService;
        this.calendarService = calendarService;
        this.workingDaysService = workingDaysService;
        this.ganttTeamService = ganttTeamService;
        this.userManager = userManager;
        this.userCalendarService = userCalendarService;
    }

    @Override
    public GanttDto getGantt(ApplicationUser user, int calendarId, String startDate, String endDate, GanttParams params) throws ParseException, SearchException, GetException {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!canUse(user, calendar)) {
            throw new SecurityException("No permission");
        }

        if (StringUtils.isEmpty(startDate))
            startDate = LocalDate.now().withDayOfYear(1).format(java.time.format.DateTimeFormatter.ISO_DATE);
        if (StringUtils.isEmpty(endDate))
            endDate = LocalDate.now().plusMonths(3).format(java.time.format.DateTimeFormatter.ISO_DATE);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(timeZoneManager.getTimeZoneforUser(user));

        Stream<EventDto> eventDtoStream;
        if (params.isIncludeUnscheduled()) {
            eventDtoStream = calendarEventService.getUnboundedEvents(calendar, params.getGroupBy(), user, true, params.getOrder(), params.getSprintId(), params.getFields(), params.isForPlan());
        } else {
            eventDtoStream = calendarEventService.getEventsWithDuration(
                calendar, params.getGroupBy(), user, true, params.getOrder(), params.getFields(),
                dateFormat.parse(startDate), dateFormat.parse(endDate)
            );
        }

        return getGantt(eventDtoStream, user, calendarId, params.getGroupBy());
    }

    @Override
    public GanttDto getGantt(ApplicationUser user, int calendarId, GanttParams params) throws Exception {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!canUse(user, calendar)) {
            throw new SecurityException("No permission");
        }

        Stream<EventDto> events = calendarEventService.getUnboundedEvents(
            calendar, params.getGroupBy(), user, true, params.getOrder(), params.getSprintId(), params.getFields(), params.isForPlan()
        );
        return getGantt(events, user, calendarId, params.getGroupBy());
    }

    private GanttDto getGantt(Stream<EventDto> eventDtoStream, ApplicationUser user, int calendarId, String groupBy) throws GetException {
        List<GanttResourceDto> resources = new ArrayList<>();
        Set<String> resourceKeys = new HashSet<>();
        List<GanttTaskDto> events = new ArrayList<>();
        Set<EventGroup> groups = new HashSet<>();

        resources.add(new GanttResourceDto("-1", "Другие", null)); // Other team
        resources.add(new GanttResourceDto("null", "Не назначен", "-1")); // Unassigned
        for (GanttTeamDto teamDto : ganttTeamService.getTeams(user, calendarId)) {
            resources.add(new GanttResourceDto(String.valueOf(teamDto.getId()), teamDto.getName(), null));
            for (GanttUserDto userDto : teamDto.getUsers()) {
                resourceKeys.add(userDto.getKey());
                resources.add(new GanttResourceDto(userDto.getKey(), userDto.getDisplayName(), String.valueOf(teamDto.getId())));
            }
        }

        boolean shouldAppendGroupToId = MULTI_GROUP_TYPES.contains(groupBy);

        eventDtoStream
                .peek(eventDto -> {
                    if (eventDto.getGroups() != null)
                        groups.addAll(eventDto.getGroups());

                    String resource = eventDto.getAssignee() != null ? eventDto.getAssignee().getKey() : "null";
                    if(!resourceKeys.contains(resource)) {
                        ApplicationUser resourceUser = userManager.getUserByKey(resource);
                        if (resourceUser != null) {
                            resourceKeys.add(resourceUser.getKey());
                            resources.add(new GanttResourceDto(resourceUser.getKey(), resourceUser.getDisplayName(), "-1"));
                        }
                    }
                })
                .filter(e -> e.getType() == EventDto.Type.ISSUE)
                .flatMap(eventDto -> buildEvenWithGroups(eventDto, user, shouldAppendGroupToId))
                .forEach(events::add);

        groups.stream()
              .map(group -> buildGroup(
                      group,
                      events.stream()
                            .filter(event -> Objects.equals(event.getParent(), group.getId()))
                            .allMatch(GanttTaskDto::getUnscheduled)
              ))
              .forEach(events::add);

        List<GanttLinkDto> links = Arrays.stream(ao.find(GanttLink.class, Query.select().where("CALENDAR_ID = ?", calendarId)))
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

        GanttCollectionsDto collectionsDto = new GanttCollectionsDto();
        collectionsDto.setResources(resources);
        collectionsDto.setLinks(links);

        GanttDto ganttDto = new GanttDto();
        ganttDto.setData(events);
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
        if (!canUse(user, calendar)) {
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
        if (!canUse(user, calendar)) {
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
    public List<GanttTaskDto> updateDates(ApplicationUser user, int calendarId, String issueKey, GanttTaskForm form, List<String> fields) throws Exception {
        return updateDates(user, calendarId, issueKey, form.getStartDate(), form.getDuration(), fields);
    }

    @Override
    public void applyPlan(ApplicationUser user, int calendarId, GanttPlanForm form) throws Exception {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!permissionService.hasAdminPermission(user, calendar)) {
            throw new SecurityException("No permission");
        }

        if (!(isMutableField(calendar.getEventStart()) && isMutableField("timeoriginalestimate"))) {
            throw new IllegalArgumentException("Calendar start and original estimate must be present and mutable");
        }

        for (GanttPlanItem item : form.getItems()) {
            calendarEventService.moveEvent(
                user,
                calendarId,
                item.getTaskId(),
                item.getStartDate(),
                null,
                item.getDuration() != null ? item.getDuration() + "m" : null
            );
        }
    }

    @Override
    public GanttTaskDto setEstimate(ApplicationUser user, int calendarId, String issueKey, GanttEstimateForm form, List<String> fields) throws Exception {
        return buildEvent(calendarEventService.moveEvent(user, calendarId, issueKey, form.getStart(), null, form.getEstimate(), fields), user, null);
    }

    @Override
    public List<String> getErrors(ApplicationUser user, int calendarId) throws GetException {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!canUse(user, calendar)) {
            throw new SecurityException("No permission");
        }

        List<String> errors = new ArrayList<>();
        UserCalendar userCalendar = userCalendarService.find(calendarId, user.getKey());
        if (userCalendar == null) {
            errors.add(i18nHelper.getText("ru.mail.jira.plugins.calendar.unavailable"));
        }

        if (!isMutableField(calendar.getEventStart())) {
            errors.add(i18nHelper.getText("ru.mail.jira.plugins.calendar.gantt.error.startImmutable"));
        }

        return errors;
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

    public List<GanttTaskDto> updateDates(ApplicationUser user, int calendarId, String issueKey, String startDate, Long duration, List<String> fields) throws Exception {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!canUse(user, calendar)) {
            throw new SecurityException("No permission");
        }

        List<GanttTaskDto> result = new ArrayList<>();

        EventDto event = calendarEventService.moveEvent(
            user,
            calendarId,
            issueKey,
            startDate,
            null,
            duration != null ? duration + "m" : null,
            fields
        );

        result.add(buildEvent(event, user, null));

        return result;
    }

    private Stream<GanttTaskDto> buildEvenWithGroups(EventDto event, ApplicationUser user, boolean addGroupId) {
        if (addGroupId) {
            if (event.getGroups() != null && !event.getGroups().isEmpty()) {
                return event
                    .getGroups()
                    .stream()
                    .map(group -> buildEvent(event, user, group.getId()));
            }
        }

        GanttTaskDto task = buildEvent(event, user, null);

        if (event.getMilestones() != null) {
            return Stream.concat(
                Stream.of(task),
                event
                    .getMilestones()
                    .stream()
                    .map(milestone -> buildMilestone(task, milestone))
            );
        }

        return Stream.of(task);
    }

    private GanttTaskDto buildMilestone(GanttTaskDto task, EventMilestoneDto milestone) {
        GanttTaskDto result = new GanttTaskDto();
        result.setStartDate(milestone.getDate());
        result.setEndDate(milestone.getDate());
        result.setSummary(task.getSummary() + " - " + milestone.getFieldName());
        result.setParent(task.getId());
        result.setId(task.getId() + "-" + milestone.getFieldId());
        result.setType("milestone");
        result.setLinkable(task.isLinkable());
        result.setUnscheduled(task.getUnscheduled());

        return result;
    }

    private GanttTaskDto buildEvent(EventDto event, ApplicationUser user, String groupId) {
        GanttTaskDto ganttTaskDto = new GanttTaskDto();
        ganttTaskDto.setType("issue");
        ganttTaskDto.setOriginalEvent(event);
        ganttTaskDto.setOpen(false);

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
            ganttTaskDto.setResizable(true); //todo: check if user can edit estimate for issue
        }
        ganttTaskDto.setEntityId(event.getId());
        ganttTaskDto.setSummary(event.getTitle());
        ganttTaskDto.setText(String.format("%s %s", event.getId(), event.getTitle()));
        ganttTaskDto.setIconSrc(event.getIssueTypeImgUrl());
        ganttTaskDto.setResolved(event.isResolved());

        IssueInfo issueInfo = event.getIssueInfo();
        if (issueInfo != null) {
            ganttTaskDto.setResource(event.getAssignee() != null ? event.getAssignee().getKey() : "null");
            ganttTaskDto.setAssignee(issueInfo.getAssignee());

            if (issueInfo.getCustomFields() != null) {
                ganttTaskDto.setFields(issueInfo.getCustomFields());
            }
        }

        ganttTaskDto.setStartDate(event.getStart());

        Date eventStart = event.getStartDate();
        Long originalEstimate = event.getOriginalEstimateSeconds();
        Long timeSpent = event.getTimeSpentSeconds();

        if (eventStart != null) {
            if (originalEstimate != null) {
                ganttTaskDto.setDuration(TimeUnit.SECONDS.toMinutes(originalEstimate));

                if (timeSpent != null) {
                    if (timeSpent > originalEstimate) {
                        ganttTaskDto.setPlannedDuration(ganttTaskDto.getDuration());
                        ganttTaskDto.setDuration(TimeUnit.SECONDS.toMinutes(timeSpent));
                        ganttTaskDto.setOverdueSeconds(timeSpent - originalEstimate);
                    } else if (event.isResolved() && timeSpent < originalEstimate) {
                        ganttTaskDto.setEarlyDuration(TimeUnit.SECONDS.toMinutes(originalEstimate - timeSpent));
                    }
                }
            } else if (event.getEndDate() != null) {
                ZoneId zoneId = timeZoneManager.getTimeZoneforUser(user).toZoneId();

                Set<LocalDate> nonWorkingDays = Arrays
                    .stream(workingDaysService.getNonWorkingDays())
                    .map(NonWorkingDay::getDate)
                    .map(date -> date.toInstant().atZone(timeZoneManager.getDefaultTimezone().toZoneId()))
                    .map(ZonedDateTime::toLocalDate)
                    .collect(Collectors.toSet());
                List<Integer> workingDays = workingDaysService.getWorkingDays();

                int durationDays = DateUtil.countWorkDays(
                    event.getStartDate().toInstant().atZone(zoneId).toLocalDate(),
                    event.getEndDate().toInstant().atZone(zoneId).toLocalDate(),
                    workingDays, nonWorkingDays
                );

                WorkingTimeDto workingTime = workingDaysService.getWorkingTime();
                ganttTaskDto.setDuration(durationDays * workingTime.getStartTime().until(workingTime.getEndTime(), ChronoUnit.MINUTES));
            }
        }

        if (ganttTaskDto.getStartDate() == null || ganttTaskDto.getDuration() == null) {
            ganttTaskDto.setUnscheduled(true);
        } else {
            ganttTaskDto.setUnscheduled(false);
        }

        if (originalEstimate != null && originalEstimate > 0 && timeSpent != null) {
            ganttTaskDto.setEstimateSeconds(originalEstimate);
            ganttTaskDto.setProgress(timeSpent * 1.0 / originalEstimate);
        }
        if (event.getOriginalEstimate() != null) {
            ganttTaskDto.setEstimate(event.getOriginalEstimate());
        }

        return ganttTaskDto;
    }

    private GanttTaskDto buildGroup(EventGroup group, boolean unscheduled) {
        GanttTaskDto result = new GanttTaskDto();

        result.setId(group.getId());
        result.setText(group.getName());
        result.setSummary(group.getName());
        result.setIconSrc(group.getAvatar());
        result.setType("group");
        result.setOpen(true);
        result.setUnscheduled(unscheduled);

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

    private boolean canUse(ApplicationUser user, Calendar calendar) {
        return permissionService.hasUsePermission(user, calendar) || permissionService.hasAdminPermission(user, calendar);
    }
}
