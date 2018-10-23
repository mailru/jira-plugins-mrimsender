package ru.mail.jira.plugins.calendar.planning;

import com.atlassian.jira.config.PriorityManager;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.lang.Pair;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.order.SortOrder;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.configuration.NonWorkingDay;
import ru.mail.jira.plugins.calendar.configuration.WorkingDaysService;
import ru.mail.jira.plugins.calendar.configuration.WorkingTimeDto;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttLinkDto;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttTaskDto;
import ru.mail.jira.plugins.calendar.service.Order;
import ru.mail.jira.plugins.calendar.service.applications.JiraSoftwareHelper;
import ru.mail.jira.plugins.calendar.service.gantt.GanttParams;
import ru.mail.jira.plugins.calendar.service.gantt.GanttService;
import ru.mail.jira.plugins.calendar.util.DateUtil;
import ru.mail.jira.plugins.calendar.util.GanttLinkType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PlanningService {
    private final DateTimeFormatter dateTimeFormatter;
    private final TimeZoneManager timeZoneManager;
    private final PriorityManager priorityManager;
    private final JiraSoftwareHelper jiraSoftwareHelper;
    private final WorkingDaysService workingDaysService;
    private final PlanningEngine planningEngine;
    private final GanttService ganttService;

    @Autowired
    public PlanningService(
            @ComponentImport DateTimeFormatter dateTimeFormatter,
            @ComponentImport TimeZoneManager timeZoneManager,
            @ComponentImport PriorityManager priorityManager,
            JiraSoftwareHelper jiraSoftwareHelper,
            WorkingDaysService workingDaysService,
            PlanningEngine planningEngine,
            GanttService ganttService
    ) {
        this.dateTimeFormatter = dateTimeFormatter;
        this.timeZoneManager = timeZoneManager;
        this.priorityManager = priorityManager;
        this.jiraSoftwareHelper = jiraSoftwareHelper;
        this.workingDaysService = workingDaysService;
        this.planningEngine = planningEngine;
        this.ganttService = ganttService;
    }

    //todo: create class with all params
    public GanttDto doPlan(ApplicationUser user, int calendarId, String deadline, GanttParams params) throws Exception {
        DateTimeFormatter dateFormat = dateTimeFormatter.forUser(user).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);

        boolean useRank = false;
        boolean usePriority = false;

        Order order = null;

        //todo: calculate estimate if no estimate and start&end is present

        if (params.getOrder() != null) {
            String orderBy = params.getOrder().getField();

            if ("priority".equals(orderBy)) {
                order = new Order(orderBy, SortOrder.DESC);
                usePriority = true;
            } else if ("rank".equals(orderBy) && jiraSoftwareHelper.isAvailable()) {
                order = new Order(jiraSoftwareHelper.getRankField().getId(), SortOrder.ASC);
                useRank = true;
            }
        }

        GanttDto ganttData = ganttService.getGantt(
            user, calendarId,
            new GanttParams(order, params.getGroupBy(), params.getSprintId(), params.getFields(), true, true)
        );

        List<GanttTaskDto> ganttEvents = ganttData.getData();
        List<EventDto> events = ganttEvents
                .stream()
                .filter(task -> "issue".equals(task.getType()))
                .map(GanttTaskDto::getOriginalEvent)
                .collect(Collectors.toList());
        List<GanttLinkDto> links = ganttData.getCollections().getLinks();

        Map<EventDto, Set<EventDto>> dependencies = new HashMap<>();

        for (GanttLinkDto link : links) {
            if (GanttLinkType.FINISH_TO_START != GanttLinkType.fromString(link.getType())) {
                continue;
            }

            EventDto source = events.stream().filter(event -> event.getId().equals(link.getSource())).findAny().orElse(null);
            EventDto target = events.stream().filter(event -> event.getId().equals(link.getTarget())).findAny().orElse(null);

            if (source != null && target != null) {
                Set<EventDto> set = dependencies.computeIfAbsent(target, (key) -> new HashSet<>());
                set.add(source);
            }
        }

        Map<EventDto, Double> priorities = new HashMap<>();

        if (useRank) {
            int i = events.size();
            double total = events.size();

            for (EventDto event : events) {
                priorities.put(event, i/total);
                ++i;
            }
        } else if (usePriority) {
            double maxSequence = priorityManager.getPriorities().stream().mapToLong(Priority::getSequence).max().orElse(1) + 1;

            for (EventDto event : events) {
                priorities.put(event, event.getIssueInfo().getPrioritySequence() / maxSequence);
            }
        }

        int workingHours = getWorkingHours();
        Map<EventDto, Integer> issueDuration =
            ganttEvents
                .stream()
                .filter(task -> "issue".equals(task.getType()))
                .collect(Collectors.toMap(
                    GanttTaskDto::getOriginalEvent,
                    event -> {
                        if (event.getDuration() != null) {
                            //todo: round or ceil value
                            return (int) TimeUnit.MINUTES.toHours(event.getDuration());
                        }
                        return workingHours;
                    }
                ));

        Set<LocalDate> nonWorkingDays = Arrays
            .stream(workingDaysService.getNonWorkingDays())
            .map(NonWorkingDay::getDate)
            .map(date -> date.toInstant().atZone(timeZoneManager.getDefaultTimezone().toZoneId()))
            .map(ZonedDateTime::toLocalDate)
            .collect(Collectors.toSet());
        List<Integer> workingDays = workingDaysService.getWorkingDays();

        Map<EventDto, Pair<Date, Date>> plan = planningEngine.generatePlan2(
                events,
                issueDuration,
                ImmutableMap.of(), //todo
                dependencies,
                priorities,
                DateUtil.countWorkDays(LocalDate.now(), LocalDate.parse(deadline), workingDays, nonWorkingDays, true),
                workingHours
        );

        plan.forEach((event, dates) -> {
            GanttTaskDto ganttTask = ganttEvents.stream().filter(e -> e.getId().equals(event.getId())).findAny().orElse(null);

            ganttTask.setStartDate(dateFormat.format(dates.first()));
            ganttTask.setDuration(TimeUnit.HOURS.toMinutes(issueDuration.get(event)));
            //ganttTask.setEndDate(dateFormat.format(dates.second()));
            ganttTask.setUnscheduled(false);
            ganttTask.setOverdueSeconds(null);
            ganttTask.setEarlyDuration(null);
        });

        return ganttData;
    }

    private int getWorkingHours() {
        WorkingTimeDto workingTime = workingDaysService.getWorkingTime();
        return (int) workingTime.getStartTime().until(workingTime.getEndTime(), ChronoUnit.HOURS);
    }


}
