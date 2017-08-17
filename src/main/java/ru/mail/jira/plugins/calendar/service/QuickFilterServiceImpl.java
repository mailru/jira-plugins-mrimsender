package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import net.java.ao.Query;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.model.QuickFilter;
import ru.mail.jira.plugins.calendar.model.FavouriteQuickFilter;

import java.util.Collection;
import java.util.Map;

@Component
public class QuickFilterServiceImpl implements QuickFilterService {
    private static final int QUICK_FILTER_NAME_MAX_LENGTH = 255;

    private final ActiveObjects ao;
    private final I18nResolver i18nResolver;
    private final SearchService searchService;

    @Autowired
    public QuickFilterServiceImpl(
        @ComponentImport ActiveObjects ao,
        @ComponentImport I18nResolver i18nResolver,
        @ComponentImport SearchService searchService
    ) {
        this.ao = ao;
        this.i18nResolver = i18nResolver;
        this.searchService = searchService;
    }

    private static JSONObject formatErrorCollection(ErrorCollection errorCollection) {
        JSONObject result = new JSONObject();
        try {
            JSONObject errorsObject = new JSONObject();
            Collection<String> errorMessages = errorCollection.getErrorMessages();
            if (errorMessages != null && errorMessages.size() > 0)
                result.put("errorMessages", errorMessages);

            if (errorCollection.getErrors() != null && errorCollection.getErrors().size() > 0)
                for (Map.Entry<String, String> entry : errorCollection.getErrors().entrySet())
                    errorsObject.put(entry.getKey(), entry.getValue());
            result.put("errors", errorsObject);
        } catch (Exception ignore) {
        }

        return result;
    }

    private void validateQuickFilter(ApplicationUser user, String name, String jql) {
        ErrorCollection errors = new SimpleErrorCollection();
        if (user == null)
            errors.addErrorMessage("User doesn't exist");
        if (StringUtils.isBlank(name))
            errors.addError("name", i18nResolver.getText("issue.field.required", i18nResolver.getText("ru.mail.jira.plugins.calendar.quick.filter.dialog.name")));
        else if (name.length() > QUICK_FILTER_NAME_MAX_LENGTH)
            errors.addError("name", i18nResolver.getText("admin.errors.userproperty.value.too.long"));

        if (StringUtils.isBlank(jql))
            errors.addError("jql", i18nResolver.getText("issue.field.required", "JQL"));
        else {
            SearchService.ParseResult parseResult = searchService.parseQuery(user, jql);
            if (!parseResult.isValid())
                errors.addError("jql", StringUtils.join(parseResult.getErrors().getErrorMessages(), "\n"));
            else {
                MessageSet validateMessages = searchService.validateQuery(user, parseResult.getQuery());
                if (validateMessages.hasAnyErrors())
                    errors.addError("jql", StringUtils.join(validateMessages.getErrorMessages(), "\n"));
            }
        }

        if (errors.hasAnyErrors())
            throw new IllegalArgumentException(formatErrorCollection(errors).toString());
    }

    private void setQuickFilterFields(QuickFilter quickFilter, String name, String jql, String description, boolean share) {
        quickFilter.setName(name);
        quickFilter.setJql(jql);
        quickFilter.setDescription(description);
        quickFilter.setShare(share);
        quickFilter.save();
    }

    @Override
    public QuickFilter getQuickFilter(int id) throws GetException {
        QuickFilter quickFilter = ao.get(QuickFilter.class, id);
        if (quickFilter == null)
            throw new GetException("No Quick Filter with id = " + id);
        return quickFilter;
    }

    @Override
    public QuickFilter findQuickFilter(int id) {
        try {
            return getQuickFilter(id);
        } catch (GetException e) {
            return null;
        }
    }

    @Override
    public QuickFilter createQuickFilter(int calendarId, String name, String jql, String description, Boolean share, ApplicationUser user) throws Exception {
        validateQuickFilter(user, name, jql);

        QuickFilter quickFilter = ao.create(QuickFilter.class);
        quickFilter.setCalendarId(calendarId);
        quickFilter.setCreatorKey(user.getKey());
        setQuickFilterFields(quickFilter, name, jql, description, share);
        return quickFilter;
    }

    @Override
    public QuickFilter updateQuickFilter(int id, int calendarId, String name, String jql, String description, Boolean share, ApplicationUser user) throws Exception {
        QuickFilter quickFilter = getQuickFilter(id);
        if (!user.getKey().equals(quickFilter.getCreatorKey()))
            throw new SecurityException(String.format("User %s can't edit filter %s", user.getDisplayName(), quickFilter.getName()));
        validateQuickFilter(user, StringUtils.defaultString(name, quickFilter.getName()), StringUtils.defaultString(jql, quickFilter.getJql()));
        setQuickFilterFields(quickFilter, StringUtils.defaultString(name, quickFilter.getName()), StringUtils.defaultString(jql, quickFilter.getJql()), StringUtils.defaultString(description, quickFilter.getDescription()), BooleanUtils.toBooleanDefaultIfNull(share, quickFilter.isShare()));
        return quickFilter;
    }

    @Override
    public void deleteQuickFilterById(int id, ApplicationUser user) throws GetException {
        QuickFilter quickFilter = getQuickFilter(id);
        if (!user.getKey().equals(quickFilter.getCreatorKey()))
            throw new SecurityException(String.format("User %s can't edit filter %s", user.getDisplayName(), quickFilter.getName()));
        ao.delete(ao.find(FavouriteQuickFilter.class, Query.select().where("QUICK_FILTER_ID = ?", quickFilter.getID())));
        ao.delete(quickFilter);
    }

    @Override
    public void deleteQuickFilterByCalendarId(int calendarId) throws GetException {
        QuickFilter[] quickFilters = ao.find(QuickFilter.class, Query.select().where("CALENDAR_ID = ?", calendarId));
        for (QuickFilter quickFilter : quickFilters) {
            ao.delete(ao.find(FavouriteQuickFilter.class, Query.select().where("QUICK_FILTER_ID = ?", quickFilter.getID())));
            ao.delete(quickFilter);
        }
    }

    @Override
    public QuickFilter[] getQuickFilters(int calendarId, ApplicationUser user, boolean share) {
        return ao.find(QuickFilter.class, Query.select().where("CALENDAR_ID = ? AND (CREATOR_KEY = ? OR SHARE = ?)", calendarId, user.getKey(), share));
    }
}
