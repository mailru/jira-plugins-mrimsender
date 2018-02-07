package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.Query;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.model.GanttLink;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.GanttCollectionsDto;
import ru.mail.jira.plugins.calendar.rest.dto.GanttDto;
import ru.mail.jira.plugins.calendar.rest.dto.GanttEventDto;
import ru.mail.jira.plugins.calendar.rest.dto.GanttLinkDto;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

@Component
public class GanttServiceImpl implements GanttService {
    private final ActiveObjects ao;
    private final CustomFieldManager customFieldManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final JiraDeprecatedService jiraDeprecatedService;
    private final CalendarEventService calendarEventService;

    @Autowired
    public GanttServiceImpl(
            @ComponentImport ActiveObjects ao,
            @ComponentImport CustomFieldManager customFieldManager,
            @ComponentImport JiraAuthenticationContext jiraAuthenticationContext,
            JiraDeprecatedService jiraDeprecatedService,
            CalendarEventService calendarEventService) {
        this.ao = ao;
        this.customFieldManager = customFieldManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.jiraDeprecatedService = jiraDeprecatedService;
        this.calendarEventService = calendarEventService;
    }

    @Override
    public GanttDto getGantt(int calendarId, String startDate, String endDate) throws ParseException, SearchException, GetException {
        DateTimeFormatter userDateTimeFormat = jiraDeprecatedService.dateTimeFormatter.forUser(jiraAuthenticationContext.getLoggedInUser()).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);
        if (StringUtils.isEmpty(startDate))
            startDate = LocalDate.now().withDayOfYear(1).toString("yyyy-MM-dd");
        if (StringUtils.isEmpty(endDate))
            endDate = LocalDate.now().plusMonths(3).toString("yyyy-MM-dd");

        GanttDto ganttDto = new GanttDto();
        List<EventDto> eventDtoList = calendarEventService.findEvents(calendarId, null, startDate, endDate, jiraAuthenticationContext.getLoggedInUser());
        ganttDto.setData(eventDtoList.stream()
                                     .map(eventDto -> {
                                         GanttEventDto ganttEventDto = new GanttEventDto();
                                         ganttEventDto.setId(eventDto.getId());
                                         ganttEventDto.setText(String.format("%s %s", eventDto.getId(), eventDto.getTitle()));
                                         ganttEventDto.setStartDate(eventDto.getStart());
                                         if (StringUtils.isNotEmpty(eventDto.getEnd())) {
                                             ganttEventDto.setEndDate(eventDto.getEnd());
                                         } else if (eventDto.getOriginalEstimateSeconds() != null) {
                                             ganttEventDto.setEndDate(userDateTimeFormat.format(DateUtils.addMilliseconds(eventDto.getStartDate(), eventDto.getOriginalEstimateSeconds().intValue())));
                                         }
                                         if (eventDto.getOriginalEstimateSeconds() != null && eventDto.getTimeSpentSeconds() != null)
                                             ganttEventDto.setProgress(eventDto.getTimeSpentSeconds() / eventDto.getOriginalEstimateSeconds());
                                         return ganttEventDto;
                                     })
                                     .toArray(GanttEventDto[]::new));
        GanttCollectionsDto collectionsDto = new GanttCollectionsDto();
        collectionsDto.setLinks(Arrays.stream(ao.find(GanttLink.class, Query.select().where("CALENDAR_ID = ?", calendarId)))
                                      .map(ganttLink -> {
                                          GanttLinkDto dto = new GanttLinkDto();
                                          dto.setId(ganttLink.getID());
                                          dto.setSource(ganttLink.getSource());
                                          dto.setTarget(ganttLink.getTarget());
                                          dto.setType(ganttLink.getType());
                                          return dto;
                                      })
                                      .toArray(GanttLinkDto[]::new));
        ganttDto.setCollections(collectionsDto);
        return ganttDto;
    }

    @Override
    public GanttLink createLink(int calendarId, String sourceKey, String targetKey, String type) {
        GanttLink ganttLink = ao.create(GanttLink.class);
        ganttLink.setCalendarId(calendarId);
        ganttLink.setSource(sourceKey);
        ganttLink.setTarget(targetKey);
        ganttLink.setType(type);
        ganttLink.save();
        return ganttLink;
    }

    @Override
    public void updateDates(int calendarId, String issueKey, String startDate, String endDate) {

    }
}
