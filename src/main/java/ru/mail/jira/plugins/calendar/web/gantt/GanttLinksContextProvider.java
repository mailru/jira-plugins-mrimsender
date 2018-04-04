package ru.mail.jira.plugins.calendar.web.gantt;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.plugin.webfragment.CacheableContextProvider;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.rest.dto.gantt.GanttLinkDto;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.calendar.service.GanttService;
import ru.mail.jira.plugins.calendar.util.GanttLinkType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Scanned
public class GanttLinksContextProvider implements CacheableContextProvider {
    private final Logger logger = LoggerFactory.getLogger(GanttLinksContextProvider.class);

    private final IssueService issueService;
    private final I18nResolver i18nResolver;
    private final VelocityRequestContextFactory velocityRequestContextFactory;
    private final GanttService ganttService;
    private final CalendarService calendarService;

    public GanttLinksContextProvider(
        @ComponentImport IssueService issueService,
        @ComponentImport I18nResolver i18nResolver,
        @ComponentImport VelocityRequestContextFactory velocityRequestContextFactory,
        GanttService ganttService,
        CalendarService calendarService
    ) {
        this.issueService = issueService;
        this.i18nResolver = i18nResolver;
        this.velocityRequestContextFactory = velocityRequestContextFactory;
        this.ganttService = ganttService;
        this.calendarService = calendarService;
    }

    @Override
    public String getUniqueContextKey(Map<String, Object> context) {
        Issue issue = (Issue) context.get("issue");
        ApplicationUser user = (ApplicationUser) context.get("user");
        return issue.getId() + "/" + (user == null ? "" : user.getName());
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException {

    }

    @Override
    public Map<String, Object> getContextMap(Map<String, Object> context) {
        HashMap<String, Object> result = new HashMap<>();

        Issue issue = (Issue) context.get("issue");
        ApplicationUser user = (ApplicationUser) context.get("user");
        String baseUrl = velocityRequestContextFactory.getJiraVelocityRequestContext().getBaseUrl();

        Map<Integer, List<GanttLinkDto>> groupedLinks = ganttService
            .getLinks(issue.getKey())
            .stream()
            .collect(Collectors.toMap(
                GanttLinkDto::getCalendarId,
                Lists::newArrayList,
                (a, b) -> {
                    a.addAll(b);
                    return a;
                }
            ));

        List<LinkGroupDto> groups = new ArrayList<>();

        groupedLinks.forEach((calendarId, links) -> {
            Calendar calendar = null;
            try {
                calendar = calendarService.getCalendar(calendarId);
            } catch (GetException e) {
                logger.warn("calendar with id {} not found", calendarId, e);
            }

            LinkGroupDto group = new LinkGroupDto();
            group.setCalendarId(calendarId);
            if (calendar != null) {
                group.setCalendarName(calendar.getName());
            } else {
                group.setCalendarName("UNAVAILABLE");
            }

            List<LinkDto> resultLinks = new ArrayList<>();

            for (GanttLinkDto link : links) {
                LinkDto linkDto = new LinkDto();

                boolean outbound = issue.getKey().equals(link.getSource());
                String issueKey = outbound ? link.getTarget() : link.getSource();

                linkDto.setIssueKey(issueKey);
                linkDto.setOutbound(outbound);
                linkDto.setTypeName(i18nResolver.getText(
                    "ru.mail.jira.plugins.calendar.gantt.linkType." + (outbound ? "out" : "in") + "." + GanttLinkType.fromString(link.getType()))
                );

                IssueService.IssueResult issueResult = issueService.getIssue(user, issueKey);

                if (issueResult.isValid()) {
                    MutableIssue otherIssue = issueResult.getIssue();
                    linkDto.setIssueSummary(otherIssue.getSummary());
                    if (otherIssue.getIssueType() != null) {
                        linkDto.setIssueTypeIconUrl(otherIssue.getIssueType().getCompleteIconUrl());
                    }
                } else {
                    linkDto.setIssueSummary("Issue unavailable");
                }

                resultLinks.add(linkDto);
            }

            group.setLinks(resultLinks);
            groups.add(group);
        });

        result.put("linkGroups", groups);
        result.put("baseUrl", baseUrl);
        result.put("i18n", i18nResolver);

        return result;
    }
}
