package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.model.GanttLink;
import ru.mail.jira.plugins.calendar.rest.dto.EventDto;
import ru.mail.jira.plugins.calendar.rest.dto.GanttCollectionsDto;
import ru.mail.jira.plugins.calendar.rest.dto.GanttDto;
import ru.mail.jira.plugins.calendar.rest.dto.GanttEventDto;
import ru.mail.jira.plugins.calendar.rest.dto.GanttLinkDto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
            @ComponentImport JiraDeprecatedService jiraDeprecatedService,
            CalendarEventService calendarEventService) {
        this.ao = ao;
        this.customFieldManager = customFieldManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.jiraDeprecatedService = jiraDeprecatedService;
        this.calendarEventService = calendarEventService;
    }

    @Override
    public GanttDto getGantt(int calendarId) throws ParseException, SearchException, GetException {
        GanttDto ganttDto = new GanttDto();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<EventDto> eventDtoList = calendarEventService.findEvents(calendarId, null, "2015-01-01", dateFormat.format(new Date()), jiraAuthenticationContext.getLoggedInUser());//todo dates params
        ganttDto.setData(eventDtoList.stream()
                                     .map(eventDto -> {
                                         GanttEventDto ganttEventDto = new GanttEventDto();
                                         ganttEventDto.setId(eventDto.getId());
                                         ganttEventDto.setText(eventDto.getTitle());
                                         ganttEventDto.setStartDate(eventDto.getStart());
                                         ganttEventDto.setEndDate(eventDto.getEnd());
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
}
