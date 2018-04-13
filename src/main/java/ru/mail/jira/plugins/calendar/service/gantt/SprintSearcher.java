package ru.mail.jira.plugins.calendar.service.gantt;

import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchProviderFactory;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.query.Query;
import org.apache.lucene.search.IndexSearcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.service.CalendarEventService;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.calendar.service.PermissionService;
import ru.mail.jira.plugins.calendar.service.applications.JiraSoftwareHelper;
import ru.mail.jira.plugins.calendar.service.applications.SprintDto;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SprintSearcher {
    private final SearchProviderFactory searchProviderFactory;
    private final SearchProvider searchProvider;
    private final CalendarEventService calendarEventService;
    private final CalendarService calendarService;
    private final PermissionService permissionService;
    private final JiraSoftwareHelper jiraSoftwareHelper;

    @Autowired
    public SprintSearcher(
        @ComponentImport SearchProviderFactory searchProviderFactory,
        @ComponentImport SearchProvider searchProvider,
        CalendarEventService calendarEventService,
        CalendarService calendarService,
        PermissionService permissionService,
        JiraSoftwareHelper jiraSoftwareHelper
    ) {
        this.searchProviderFactory = searchProviderFactory;
        this.searchProvider = searchProvider;
        this.calendarEventService = calendarEventService;
        this.calendarService = calendarService;
        this.permissionService = permissionService;
        this.jiraSoftwareHelper = jiraSoftwareHelper;
    }

    public List<SprintDto> findSprintsForCalendar(ApplicationUser user, int calendarId) throws SearchException, GetException {
        Calendar calendar = calendarService.getCalendar(calendarId);

        if (!permissionService.hasUsePermission(user, calendar) && !permissionService.hasAdminPermission(user, calendar)) {
            throw new SecurityException("No permission");
        }

        Query query = calendarEventService.getUnboundedEventsQuery(user, calendar, null, null, false);

        IndexSearcher searcher = searchProviderFactory.getSearcher(SearchProviderFactory.ISSUE_INDEX);

        SprintCollector collector = new SprintCollector(jiraSoftwareHelper.getSprintField(), searcher);
        searchProvider.searchOverrideSecurity(query, user, collector);

        return collector
            .getSprintIds()
            .stream()
            .map(id -> jiraSoftwareHelper.getSprint(user, id))
            .filter(Objects::nonNull)
            .filter(sprint -> sprint.getState() != SprintDto.State.CLOSED)
            .sorted(Comparator.comparing(SprintDto::getState).reversed())
            .collect(Collectors.toList());
    }
}
