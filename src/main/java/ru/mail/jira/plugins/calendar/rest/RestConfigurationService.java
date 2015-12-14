package ru.mail.jira.plugins.calendar.rest;

import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.sharing.SharedEntityColumn;
import com.atlassian.jira.sharing.search.SharedEntitySearchContext;
import com.atlassian.jira.sharing.search.SharedEntitySearchParameters;
import com.atlassian.jira.sharing.search.SharedEntitySearchParametersBuilder;
import com.atlassian.jira.sharing.search.SharedEntitySearchResult;
import com.atlassian.jira.util.I18nHelper;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.calendar.rest.dto.AllSources;
import ru.mail.jira.plugins.calendar.rest.dto.SourceField;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.commons.RestExecutor;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/calendar/config")
@Produces(MediaType.APPLICATION_JSON)
public class RestConfigurationService {
    private final CustomFieldManager customFieldManager;
    private final I18nHelper i18nHelper;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final ProjectService projectService;
    private final SearchRequestService searchRequestService;

    public RestConfigurationService(CustomFieldManager customFieldManager,
                                    I18nHelper i18nHelper,
                                    JiraAuthenticationContext jiraAuthenticationContext,
                                    ProjectService projectService,
                                    SearchRequestService searchRequestService) {
        this.customFieldManager = customFieldManager;
        this.i18nHelper = i18nHelper;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.projectService = projectService;
        this.searchRequestService = searchRequestService;
    }

    @GET
    @Path("/displayedFields")
    public Map<String, String> getDisplayedFields() {
        Map<String, String> result = new LinkedHashMap<String, String>(CalendarService.DISPLAYED_FIELDS.size());
        for (String field : CalendarService.DISPLAYED_FIELDS)
            result.put(field, i18nHelper.getText(field));
        for (CustomField customField : customFieldManager.getCustomFieldObjects())
            result.put(customField.getId(), customField.getName());
        return result;
    }

    @GET
    @Path("/eventSources")
    public Response getEventSources(@QueryParam("filter") final String filter) {
        return new RestExecutor<AllSources>() {
            @Override
            protected AllSources doAction() throws Exception {
                return new AllSources(getProjectSources(filter), getFilterSources(filter));
            }
        }.getResponse();
    }

    private List<SourceField> getProjectSources(String filter) {
        List<SourceField> result = new ArrayList<SourceField>();
        filter = filter.trim().toLowerCase();
        int length = 0;
        List<Project> allProjects = projectService.getAllProjects(jiraAuthenticationContext.getUser()).get();
        for (Project project : allProjects) {
            if (project.getName().toLowerCase().contains(filter) || project.getKey().toLowerCase().contains(filter)) {
                result.add(new SourceField("project_" + project.getId(), String.format("%s (%s)", project.getName(), project.getKey()), project.getAvatar().getId()));
                length++;
                if (length == 10)
                    break;
            }
        }

        Collections.sort(result, new Comparator<SourceField>() {
            @Override
            public int compare(SourceField fProject, SourceField sProject) {
                return fProject.getText().toLowerCase().compareTo(sProject.getText().toLowerCase());
            }
        });

        return result;
    }

    private List<SourceField> getFilterSources(String filter) {
        List<SourceField> result = new ArrayList<SourceField>();
        SharedEntitySearchParametersBuilder builder = new SharedEntitySearchParametersBuilder();
        builder.setName(StringUtils.isBlank(filter) ? null : filter);
        builder.setTextSearchMode(SharedEntitySearchParameters.TextSearchMode.WILDCARD);
        builder.setSortColumn(SharedEntityColumn.NAME, true);
        builder.setEntitySearchContext(SharedEntitySearchContext.USE);

        SharedEntitySearchResult<SearchRequest> searchResults = searchRequestService.search(new JiraServiceContextImpl(jiraAuthenticationContext.getUser()), builder.toSearchParameters(), 0, 10);

        for (SearchRequest search : searchResults.getResults())
            result.add(new SourceField("filter_" + search.getId(), search.getName(), 0));

        return result;
    }
}
