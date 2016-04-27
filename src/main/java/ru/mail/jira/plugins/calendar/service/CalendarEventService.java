package ru.mail.jira.plugins.calendar.service;

import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.customfields.impl.DateCFType;
import com.atlassian.jira.issue.customfields.impl.DateTimeCFType;
import com.atlassian.jira.issue.fields.AssigneeSystemField;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.LabelsSystemField;
import com.atlassian.jira.issue.fields.ReporterSystemField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.jql.builder.JqlClauseBuilder;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.jira.web.bean.PagerFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.rest.dto.Event;
import ru.mail.jira.plugins.calendar.rest.dto.IssueInfo;
import ru.mail.jira.plugins.commons.CommonUtils;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class CalendarEventService {
    private final static Logger log = LoggerFactory.getLogger(CalendarEventService.class);

    private final static int MILLIS_IN_DAY = 86400000;

    public static final String CREATED_DATE_KEY = "created";
    public static final String UPDATED_DATE_KEY = "updated";
    public static final String RESOLVED_DATE_KEY = "resolved";
    public static final String DUE_DATE_KEY = "due_date";

    private CalendarService calendarService;
    private CustomFieldManager customFieldManager;
    private DateTimeFormatter dateTimeFormatter;
    private FieldLayoutManager fieldLayoutManager;
    private IssueService issueService;
    private RendererManager rendererManager;
    private SearchRequestService searchRequestService;
    private SearchProvider searchProvider;
    private SearchService searchService;

    public void setCalendarService(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    public void setCustomFieldManager(CustomFieldManager customFieldManager) {
        this.customFieldManager = customFieldManager;
    }

    public void setDateTimeFormatter(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public void setFieldLayoutManager(FieldLayoutManager fieldLayoutManager) {
        this.fieldLayoutManager = fieldLayoutManager;
    }

    public void setIssueService(IssueService issueService) {
        this.issueService = issueService;
    }

    public void setRendererManager(RendererManager rendererManager) {
        this.rendererManager = rendererManager;
    }

    public void setSearchRequestService(SearchRequestService searchRequestService) {
        this.searchRequestService = searchRequestService;
    }

    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public List<Event> findEvents(final int calendarId,
                                  final String start,
                                  final String end,
                                  final ApplicationUser user) throws ParseException, SearchException, GetException {
        return findEvents(calendarId, start, end, user, false);
    }

    public List<Event> findEvents(final int calendarId,
                                  final String start,
                                  final String end,
                                  final ApplicationUser user,
                                  final boolean includeIssueInfo) throws ParseException, SearchException, GetException {
        if (log.isDebugEnabled())
            log.debug("findEvents with params. calendarId={}, start={}, end={}, user={}, includeIssueInfo={}", new Object[]{calendarId, start, end, user.toString(), includeIssueInfo});
        Calendar calendarModel = calendarService.getCalendar(calendarId);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String source = calendarModel.getSource();
        if (source.startsWith("project_"))
            return getProjectEvents(calendarModel, Long.parseLong(source.substring("project_".length())),
                                    calendarModel.getEventStart(), calendarModel.getEventEnd(), dateFormat.parse(start), dateFormat.parse(end), user, includeIssueInfo);
        else if (source.startsWith("filter_"))
            return getFilterEvents(calendarModel, Long.parseLong(source.substring("filter_".length())),
                                   calendarModel.getEventStart(), calendarModel.getEventEnd(), dateFormat.parse(start), dateFormat.parse(end), user, includeIssueInfo);
        else if (source.startsWith("jql_"))
            return getJqlEvents(calendarModel, StringUtils.substringAfter(source, "jql_"),
                                calendarModel.getEventStart(), calendarModel.getEventEnd(), dateFormat.parse(start), dateFormat.parse(end), user, includeIssueInfo);
        else {
            return Collections.emptyList();
        }
    }

    public IssueInfo getEventInfo(ApplicationUser user, int calendarId, String eventId) throws GetException {
        Calendar calendar = calendarService.getCalendar(calendarId);
        IssueService.IssueResult issueResult = issueService.getIssue(ApplicationUsers.toDirectoryUser(user), eventId);
        MutableIssue issue = issueResult.getIssue();
        return getEventInfo(calendar, issue);
    }

    private List<Event> getProjectEvents(Calendar calendar, long projectId,
                                         String startField, String endField,
                                         Date startTime, Date endTime,
                                         ApplicationUser user, boolean includeIssueInfo) throws SearchException {
        JqlClauseBuilder jqlBuilder = JqlQueryBuilder.newClauseBuilder();
        jqlBuilder.project(projectId);
        return getEvents(calendar, jqlBuilder, startField, endField, startTime, endTime, user, includeIssueInfo);
    }

    private List<Event> getFilterEvents(Calendar calendar,
                                        long filterId,
                                        String startField,
                                        String endField,
                                        Date startTime,
                                        Date endTime,
                                        ApplicationUser user, boolean includeIssueInfo) throws SearchException {
        if (log.isDebugEnabled())
            log.debug("getFilterEvents with params. calendar={}, filterId={}, startField={}, endField={}, startTime={}, endTime={}, user={}, includeIssueInfo={}",
                      new Object[]{calendar, filterId, startField, endField, startTime, endTime, user, includeIssueInfo});
        JiraServiceContext jsCtx = new JiraServiceContextImpl(user);
        SearchRequest filter = searchRequestService.getFilter(jsCtx, filterId);

        if (log.isDebugEnabled())
            log.debug("find filter by id. filter={}", filter);
        if (filter == null) {
            log.error("Filter with id => " + filterId + " is null. Maybe it was deleted");
            return new ArrayList<Event>(0);
        }

        JqlClauseBuilder jqlBuilder = JqlQueryBuilder.newClauseBuilder(filter.getQuery());
        return getEvents(calendar, jqlBuilder, startField, endField, startTime, endTime, user, includeIssueInfo);
    }

    private List<Event> getJqlEvents(Calendar calendar,
                                     String jql,
                                     String startField,
                                     String endField,
                                     Date startTime,
                                     Date endTime,
                                     ApplicationUser user, boolean includeIssueInfo) throws SearchException {
        if (log.isDebugEnabled())
            log.debug("getJqlEvents with params. calendar={}, jql={}, startField={}, endField={}, startTime={}, endTime={}, user={}, includeIssueInfo={}",
                      new Object[]{calendar, jql, startField, endField, startTime, endTime, user, includeIssueInfo});
        if (log.isDebugEnabled())
            log.debug("find filter by jql. jql={}", jql);
        if (jql == null) {
            log.error("JQL => {} is null.", jql);
            return new ArrayList<Event>(0);
        }
        SearchService.ParseResult parseResult = searchService.parseQuery(ApplicationUsers.toDirectoryUser(user), jql);

        if (parseResult.isValid()) {
            JqlClauseBuilder jqlBuilder = JqlQueryBuilder.newClauseBuilder(parseResult.getQuery());
            return getEvents(calendar, jqlBuilder, startField, endField, startTime, endTime, user, includeIssueInfo);
        } else {
            log.error("JQL is invalid => {}", jql);
            return new ArrayList<Event>(0);
        }
    }

    private List<Event> getEvents(Calendar calendar,
                                  JqlClauseBuilder jqlBuilder,
                                  String startField,
                                  String endField,
                                  Date startTime,
                                  Date endTime,
                                  ApplicationUser user, boolean includeIssueInfo) throws SearchException {
        List<Event> result = new ArrayList<Event>();

        CustomField startCF = null;
        if (startField.startsWith("customfield_")) {
            startCF = customFieldManager.getCustomFieldObject(startField);
            if (log.isDebugEnabled())
                log.debug("find customfield for startField. startCF={}", startCF);
            if (startCF == null)
                throw new IllegalArgumentException("Bad custom field id => " + startField);
        }

        CustomField endCF = null;
        if (StringUtils.isNotEmpty(endField) && endField.startsWith("customfield_")) {
            endCF = customFieldManager.getCustomFieldObject(endField);
            if (log.isDebugEnabled())
                log.debug("find customfield for endField. endCF={}", endCF);
            if (endCF == null)
                throw new IllegalArgumentException("Bad custom field id => " + endField);
        }
        DateTimeFormatter userDateFormat = dateTimeFormatter.forUser(user.getDirectoryUser()).withStyle(DateTimeStyle.ISO_8601_DATE);
        DateTimeFormatter userDateTimeFormat = dateTimeFormatter.forUser(user.getDirectoryUser()).withStyle(DateTimeStyle.ISO_8601_DATE_TIME);

        jqlBuilder.and().sub();
        addDateCondition(startField, startTime, endTime, jqlBuilder, userDateFormat);
        if (StringUtils.isNotEmpty(endField)) {
            jqlBuilder.or();
            addDateCondition(endField, startTime, endTime, jqlBuilder, userDateFormat);
            jqlBuilder.or();
            addStartLessCondition(startField, startTime, jqlBuilder, userDateFormat);
            jqlBuilder.and();
            addEndGreaterCondition(endField, endTime, jqlBuilder, userDateFormat);
        }
        jqlBuilder.endsub();
        boolean dateFieldsIsDraggable = isDateFieldsDraggable(startField, endField);
        if (log.isDebugEnabled())
            log.debug("dateFieldsIsDraggable={}", dateFieldsIsDraggable);

        List<Issue> issues = searchProvider.search(jqlBuilder.buildQuery(), user, PagerFilter.getUnlimitedFilter()).getIssues();
        if (log.isDebugEnabled())
            log.debug("searchProvider.search(). query={}, user={}, issues.size()={}", new Object[]{jqlBuilder.buildQuery().toString(), user, issues.size()});
        for (Issue issue : issues) {
            try {
                Date startDate = startCF == null ? retrieveDateByField(issue, startField) : retrieveDateByField(issue, startCF);
                if (log.isDebugEnabled())
                    log.debug("Issue startDate={}", startDate);
                Date endDate = null;
                if (StringUtils.isNotEmpty(endField))
                    endDate = endCF == null ? retrieveDateByField(issue, endField) : retrieveDateByField(issue, endCF);
                if (log.isDebugEnabled())
                    log.debug("Issue endDate={}", endDate);

                boolean isAllDay = isAllDayEvent(startCF, endCF, startField, endField);
                if (log.isDebugEnabled())
                    log.debug("Issue isAllDay={}", isAllDay);

                Event event = new Event();
                event.setCalendarId(calendar.getID());
                event.setId(issue.getKey());
                event.setTitle(issue.getSummary());
                event.setColor(calendar.getColor());
                event.setAllDay(isAllDay);

                if (startDate == null && endDate == null) { // Something unbelievable
                    log.error("Event " + issue.getKey() + " doesn't contain startDate and endDate");
                    continue;
                }

                DateTimeFormatter startFormatter = startField.equals(DUE_DATE_KEY) || startCF != null && startCF.getCustomFieldType() instanceof DateCFType ? userDateFormat.withSystemZone() : userDateTimeFormat;
                DateTimeFormatter endFormatter = endField != null && endField.equals(DUE_DATE_KEY) || endCF != null && endCF.getCustomFieldType() instanceof DateCFType ? userDateFormat.withSystemZone() : userDateTimeFormat;
                if (startDate != null) {
                    event.setStart(startFormatter.format(startDate));
                    if (endDate != null)
                        if (endField.equals(DUE_DATE_KEY) || endCF != null && endCF.getCustomFieldType() instanceof DateCFType)
                            event.setEnd(endFormatter.format(new Date(endDate.getTime() + MILLIS_IN_DAY)));
                        else
                            event.setEnd(endFormatter.format(endDate));
                } else
                    event.setStart(endFormatter.format(endDate));

                event.setStartEditable(dateFieldsIsDraggable && issueService.isEditable(issue, ApplicationUsers.toDirectoryUser(user)));
                event.setDurationEditable(isDateFieldResizable(endField) && startDate != null && endDate != null && issueService.isEditable(issue, ApplicationUsers.toDirectoryUser(user)));

                if (includeIssueInfo)
                    event.setIssueInfo(getEventInfo(calendar, issue));

                result.add(event);
            } catch (Exception e) {
                log.error(String.format("Error while trying to translate issue => %s to event", issue.getKey()), e);
            }
        }
        return result;
    }

    private IssueInfo getEventInfo(Calendar calendar, Issue issue) {
        IssueInfo result = new IssueInfo(issue.getKey(), issue.getSummary());
        result.setStatusColor(issue.getStatusObject().getStatusCategory().getColorName());

        if (StringUtils.isNotEmpty(calendar.getDisplayedFields()))
            fillDisplayedFields(result, calendar.getDisplayedFields().split(","), issue);
        return result;
    }

    private void addDateCondition(String field, Date startTime, Date endTime, JqlClauseBuilder jcb, DateTimeFormatter dateTimeFormatter) {
        if (field.equals(DUE_DATE_KEY))
            jcb.dueBetween(dateTimeFormatter.format(startTime), dateTimeFormatter.format(endTime));
        else if (field.equals(CREATED_DATE_KEY))
            jcb.createdBetween(dateTimeFormatter.format(startTime), dateTimeFormatter.format(endTime));
        else if (field.equals(UPDATED_DATE_KEY))
            jcb.updatedBetween(dateTimeFormatter.format(startTime), dateTimeFormatter.format(endTime));
        else if (field.equals(RESOLVED_DATE_KEY))
            jcb.resolutionDateBetween(dateTimeFormatter.format(startTime), dateTimeFormatter.format(endTime));
        else if (field.startsWith("customfield_")) {
            CustomField customField = customFieldManager.getCustomFieldObject(field);
            if (log.isDebugEnabled())
                log.debug("add DateRangeCondition for customfield. customField={}, start={}, end={}", new Object[]{customField, startTime, endTime});
            if (customField == null)
                throw new IllegalArgumentException("Bad custom field id => " + field);
            jcb.customField(customField.getIdAsLong()).range(dateTimeFormatter.format(startTime), dateTimeFormatter.format(endTime));
        } else
            throw new IllegalArgumentException("Bad field => " + field);
    }

    private void addStartLessCondition(String startField, Date startTime, JqlClauseBuilder jcb, DateTimeFormatter dateTimeFormatter) {
        if (startField.equals(DUE_DATE_KEY))
            jcb.due().ltEq(dateTimeFormatter.format(startTime));
        else if (startField.equals(CREATED_DATE_KEY))
            jcb.created().ltEq(dateTimeFormatter.format(startTime));
        else if (startField.equals(UPDATED_DATE_KEY))
            jcb.updated().ltEq(dateTimeFormatter.format(startTime));
        else if (startField.equals(RESOLVED_DATE_KEY))
            jcb.resolutionDate().ltEq(dateTimeFormatter.format(startTime));
        else if (startField.startsWith("customfield_")) {
            CustomField customField = customFieldManager.getCustomFieldObject(startField);
            if (log.isDebugEnabled())
                log.debug("add DateRangeCondition for customfield. customField={}, start={}", new Object[]{customField, startTime});
            if (customField == null)
                throw new IllegalArgumentException("Bad custom field id => " + startField);
            jcb.customField(customField.getIdAsLong()).ltEq(dateTimeFormatter.format(startTime));
        } else
            throw new IllegalArgumentException("Bad field => " + startField);
    }

    private void addEndGreaterCondition(String endField, Date endTime, JqlClauseBuilder jcb, DateTimeFormatter dateTimeFormatter) {
        if (endField.equals(DUE_DATE_KEY))
            jcb.due().gtEq(dateTimeFormatter.format(endTime));
        else if (endField.equals(CREATED_DATE_KEY))
            jcb.created().gtEq(dateTimeFormatter.format(endTime));
        else if (endField.equals(UPDATED_DATE_KEY))
            jcb.updated().gtEq(dateTimeFormatter.format(endTime));
        else if (endField.equals(RESOLVED_DATE_KEY))
            jcb.resolutionDate().gtEq(dateTimeFormatter.format(endTime));
        else if (endField.startsWith("customfield_")) {
            CustomField customField = customFieldManager.getCustomFieldObject(endField);
            if (log.isDebugEnabled())
                log.debug("add DateRangeCondition for customfield. customField={}, start={}", new Object[]{customField, endTime});
            if (customField == null)
                throw new IllegalArgumentException("Bad custom field id => " + endField);
            jcb.customField(customField.getIdAsLong()).gtEq(dateTimeFormatter.format(endTime));
        } else
            throw new IllegalArgumentException("Bad field => " + endField);
    }

    private boolean isAllDayEvent(@Nullable CustomField startCF, @Nullable CustomField endCF,
                                  @Nullable String startField, @Nullable String endField) {
        boolean startFieldForDate;
        if (startCF != null)
            startFieldForDate = isDateField(startCF);
        else if (startField != null)
            startFieldForDate = startField.equals(DUE_DATE_KEY);
        else
            throw new IllegalArgumentException("Event start can not be null");

        boolean endFieldForDate;
        if (endCF != null) {
            endFieldForDate = isDateField(endCF);
        } else if (endField != null) {
            endFieldForDate = endField.equals(DUE_DATE_KEY);
        } else {
            return startFieldForDate;
        }

        return endFieldForDate && startFieldForDate;
    }

    public void dragEvent(ApplicationUser user, Calendar calendar, Issue issue, long millisDelta) throws Exception {
        if (isDateFieldsNotDraggable(calendar.getEventStart(), calendar.getEventEnd()))
            throw new IllegalArgumentException(String.format("Can not drag event with key => %s, because it contains not draggable event date field", issue.getKey()));

        CustomField eventStartCF = null;
        Timestamp eventStartCFValue = null;
        boolean eventStartIsDueDate = false;
        if (calendar.getEventStart().startsWith("customfield_")) {
            eventStartCF = customFieldManager.getCustomFieldObject(calendar.getEventStart());
            if (eventStartCF == null)
                throw new IllegalStateException("Can not find custom field => " + calendar.getEventStart()); //todo:xxx may be not stateException
            eventStartCFValue = (Timestamp) issue.getCustomFieldValue(eventStartCF);
        } else
            eventStartIsDueDate = true;

        CustomField eventEndCF = null;
        Timestamp eventEndCFValue = null;
        boolean eventEndIsDueDate = false;
        if (StringUtils.isNotBlank(calendar.getEventEnd())) {
            if (calendar.getEventEnd().startsWith("customfield_")) {
                eventEndCF = customFieldManager.getCustomFieldObject(calendar.getEventEnd());
                if (eventEndCF == null)
                    throw new IllegalStateException("Can not find custom field => " + calendar.getEventStart()); //todo:xxx may be not stateException
                eventEndCFValue = (Timestamp) issue.getCustomFieldValue(eventEndCF);
            } else
                eventEndIsDueDate = true;
        }

        IssueInputParameters issueInputParams = issueService.newIssueInputParameters();

        DateTimeFormatter datePickerFormat = dateTimeFormatter.forUser(user.getDirectoryUser()).withStyle(DateTimeStyle.DATE_PICKER);
        DateTimeFormatter dateTimePickerFormat = dateTimeFormatter.forUser(user.getDirectoryUser()).withStyle(DateTimeStyle.DATE_TIME_PICKER);

        if (eventStartIsDueDate) {
            Date newDueDate = getNewDate(issue.getDueDate(), millisDelta);
            issueInputParams.setDueDate(datePickerFormat.format(newDueDate));
        } else if (eventStartCFValue != null) {
            Date value = getNewDate(eventStartCFValue, millisDelta);
            DateTimeFormatter formatter = eventStartCF.getCustomFieldType() instanceof DateTimeCFType ? dateTimePickerFormat : datePickerFormat;
            issueInputParams.addCustomFieldValue(eventStartCF.getIdAsLong(), formatter.format(value));
        }

        if (eventEndIsDueDate) {
            Date newDueDate = getNewDate(issue.getDueDate(), millisDelta);
            issueInputParams.setDueDate(datePickerFormat.format(newDueDate));
        } else if (eventEndCF != null && eventEndCFValue != null) {
            Date value = getNewDate(eventEndCFValue, millisDelta);
            DateTimeFormatter formatter = eventEndCF.getCustomFieldType() instanceof DateTimeCFType ? dateTimePickerFormat : datePickerFormat;
            issueInputParams.addCustomFieldValue(eventEndCF.getIdAsLong(), formatter.format(value));
        }

        IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(user.getDirectoryUser(), issue.getId(), issueInputParams);
        if (!updateValidationResult.isValid())
            throw new Exception(CommonUtils.formatErrorCollection(updateValidationResult.getErrorCollection()));

        IssueService.IssueResult updateResult = issueService.update(user.getDirectoryUser(), updateValidationResult);
        if (!updateResult.isValid())
            throw new Exception(CommonUtils.formatErrorCollection(updateResult.getErrorCollection()));
    }

    private Date getNewDate(Date date, long millisDelta) {
        return new Date(date.getTime() + millisDelta);
    }

    public void resizeEvent(ApplicationUser user, Calendar calendar, Issue issue, long millisDelta) throws Exception {
        if (isDateFieldNotDraggable(calendar.getEventEnd()))
            throw new IllegalArgumentException(String.format("Can not resize event with key => %s, because it contains not draggable end field", issue.getKey()));

        CustomField eventStartCF;
        Date eventStartDateValue;
        if (calendar.getEventStart().startsWith("customfield_")) {
            eventStartCF = customFieldManager.getCustomFieldObject(calendar.getEventStart());
            if (eventStartCF == null)
                throw new IllegalStateException("Can not find custom field => " + calendar.getEventStart()); //todo:xxx may be not stateException
            eventStartDateValue = (Timestamp) issue.getCustomFieldValue(eventStartCF);
        } else {
            eventStartDateValue = retrieveDateByField(issue, calendar.getEventStart());
        }

        if (eventStartDateValue == null)
            throw new IllegalArgumentException("Can not resize event with empty start date field. Issue => " + issue.getKey());


        CustomField eventEndCF = null;
        Date eventEndDateValue = null;
        boolean eventEndIsDueDate = false;
        if (StringUtils.isNotEmpty(calendar.getEventEnd())) {
            if (calendar.getEventEnd().startsWith("customfield_")) {
                eventEndCF = customFieldManager.getCustomFieldObject(calendar.getEventEnd());
                if (eventEndCF == null)
                    throw new IllegalStateException("Can not find custom field => " + calendar.getEventStart()); //todo:xxx may be not stateException
                eventEndDateValue = (Timestamp) issue.getCustomFieldValue(eventEndCF);
            } else {
                eventEndIsDueDate = true;
                eventEndDateValue = retrieveDateByField(issue, calendar.getEventEnd());
            }
        }

        if (eventEndDateValue == null)
            throw new IllegalArgumentException("Can not resize event with empty end date field. Issue => " + issue.getKey());

        if (Math.abs(millisDelta) < MILLIS_IN_DAY && (eventEndIsDueDate || eventEndCF != null && eventEndCF.getCustomFieldType() instanceof DateCFType)) {
            return;
        }

        IssueInputParameters issueInputParams = issueService.newIssueInputParameters();

        DateTimeFormatter datePickerFormat = dateTimeFormatter.forUser(user.getDirectoryUser()).withStyle(DateTimeStyle.DATE_PICKER);
        DateTimeFormatter dateTimePickerFormat = dateTimeFormatter.forUser(user.getDirectoryUser()).withStyle(DateTimeStyle.DATE_TIME_PICKER);

        if (eventEndIsDueDate) {
            Date newDueDate = getNewDate(issue.getDueDate(), millisDelta);
            issueInputParams.setDueDate(datePickerFormat.format(newDueDate));
        } else {
            DateTimeFormatter formatter = eventEndCF.getCustomFieldType() instanceof DateTimeCFType ? dateTimePickerFormat : datePickerFormat;
            Date value = getNewDate(eventEndDateValue, millisDelta);
            issueInputParams.addCustomFieldValue(eventEndCF.getIdAsLong(), formatter.format(value));
        }

        IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(ApplicationUsers.toDirectoryUser(user), issue.getId(), issueInputParams);
        if (!updateValidationResult.isValid())
            throw new Exception(CommonUtils.formatErrorCollection(updateValidationResult.getErrorCollection()));

        IssueService.IssueResult updateResult = issueService.update(ApplicationUsers.toDirectoryUser(user), updateValidationResult);
        if (!updateResult.isValid())
            throw new Exception(CommonUtils.formatErrorCollection(updateResult.getErrorCollection()));
    }

    private Date retrieveDateByField(Issue issue, String field) {
        if (field.equals(DUE_DATE_KEY))
            return issue.getDueDate();
        if (field.equals(CREATED_DATE_KEY))
            return issue.getCreated();
        if (field.equals(UPDATED_DATE_KEY))
            return issue.getUpdated();
        if (field.equals(RESOLVED_DATE_KEY))
            return issue.getResolutionDate();
        throw new IllegalArgumentException("Unknown field => " + field);
    }

    private Date retrieveDateByField(Issue issue, CustomField customField) {
        if (log.isDebugEnabled())
            log.debug("retrieveDateByField with params. issue={}, customField={}, customFieldType={}", new Object[]{issue, customField, customField.getClass()});
        if (!(customField.getCustomFieldType() instanceof com.atlassian.jira.issue.fields.DateField))
            throw new IllegalArgumentException("Bad date time => " + customField.getName());
        return (Date) issue.getCustomFieldValue(customField);
    }

    private boolean isDateTimeField(CustomField cf) {
        return cf.getCustomFieldType() instanceof DateTimeCFType;
    }

    private boolean isDateField(CustomField cf) {
        return !isDateTimeField(cf);
    }

    private boolean isDateFieldResizable(String field) {
        return !CREATED_DATE_KEY.equals(field) && !UPDATED_DATE_KEY.equals(field) && !RESOLVED_DATE_KEY.equals(field);
    }

    private boolean isDateFieldNotDraggable(String field) {
        return !isDateFieldResizable(field);
    }

    private boolean isDateFieldsDraggable(String startField, String endField) {
        return isDateFieldResizable(startField) && (isDateFieldResizable(endField) || StringUtils.isEmpty(endField));
    }

    private boolean isDateFieldsNotDraggable(String startField, String endField) {
        return !isDateFieldsDraggable(startField, endField);
    }

    private void fillDisplayedFields(IssueInfo issueInfo, String[] extraFields, Issue issue) {
        DateTimeFormatter userDateTimeFormatter = dateTimeFormatter.forLoggedInUser();
        for (String extraField : extraFields) {
            if (extraField.startsWith("customfield_")) {
                CustomField customField = customFieldManager.getCustomFieldObject(extraField);
                FieldLayoutItem fieldLayoutItem = fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem(customField);
                if (customField != null) {
                    String columnViewHtml = customField.getColumnViewHtml(fieldLayoutItem, new HashMap<String, Object>(), issue);
                    if (StringUtils.isNotEmpty(columnViewHtml))
                        issueInfo.addCustomField(customField.getName(), columnViewHtml);
                }
            } else if (extraField.equals(CalendarServiceImpl.REPORTER)) {
                if (issue.getReporter() != null) {
                    FieldLayoutItem reporterLayoutItem = fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem("reporter");


                    String columnViewHtml = ((ReporterSystemField) reporterLayoutItem.getOrderableField()).getColumnViewHtml(reporterLayoutItem, new HashMap(), issue);
                    issueInfo.setReporter(columnViewHtml);
                }
            } else if (extraField.equals(CalendarServiceImpl.ASSIGNEE)) {
                if (issue.getAssignee() != null) {
                    FieldLayoutItem assigneeLayoutItem = fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem("assignee");
                    String columnViewHtml = ((AssigneeSystemField) assigneeLayoutItem.getOrderableField()).getColumnViewHtml(assigneeLayoutItem, new HashMap(), issue);
                    issueInfo.setAssignee(columnViewHtml);
                }
            } else if (extraField.equals(CalendarServiceImpl.STATUS))
                issueInfo.setStatus(issue.getStatusObject().getName());
            else if (extraField.equals(CalendarServiceImpl.LABELS)) {
                if (issue.getLabels() != null && !issue.getLabels().isEmpty()) {
                    FieldLayoutItem labelsLayoutItem = fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem("labels");
                    String columnViewHtml = ((LabelsSystemField) labelsLayoutItem.getOrderableField()).getColumnViewHtml(labelsLayoutItem, new HashMap(), issue);
                    issueInfo.setLabels(columnViewHtml);
                }
            } else if (extraField.equals(CalendarServiceImpl.COMPONENTS) && issue.getComponentObjects() != null && !issue.getComponentObjects().isEmpty()) {
                List<String> components = new ArrayList<String>();
                for (ProjectComponent pc : issue.getComponentObjects())
                    components.add(pc.getName());
                issueInfo.setComponents(components.toString());
            } else if (extraField.equals(CalendarServiceImpl.DUEDATE) && issue.getDueDate() != null)
                issueInfo.setDueDate(userDateTimeFormatter.forLoggedInUser().withSystemZone().withStyle(DateTimeStyle.ISO_8601_DATE).format(issue.getDueDate()));
            else if (extraField.equals(CalendarServiceImpl.ENVIRONMENT) && issue.getEnvironment() != null)
                issueInfo.setEnvironment(issue.getEnvironment());
            else if (extraField.equals(CalendarServiceImpl.PRIORITY) && issue.getPriorityObject() != null) {
                issueInfo.setPriority(issue.getPriorityObject().getName());
                issueInfo.setPriorityIconUrl(issue.getPriorityObject().getIconUrl());
            } else if (extraField.equals(CalendarServiceImpl.RESOLUTION) && issue.getResolutionObject() != null)
                issueInfo.setResolution(issue.getResolutionObject().getName());
            else if (extraField.equals(CalendarServiceImpl.AFFECT) && issue.getAffectedVersions() != null && !issue.getAffectedVersions().isEmpty()) {
                List<String> affectVersions = new ArrayList<String>();
                for (Version ver : issue.getAffectedVersions())
                    affectVersions.add(ver.getName());
                issueInfo.setAffect(affectVersions.toString());
            } else if (extraField.equals(CalendarServiceImpl.CREATED))
                issueInfo.setCreated(userDateTimeFormatter.format(issue.getCreated()));
            else if (extraField.equals(CalendarServiceImpl.UPDATED))
                issueInfo.setUpdated(userDateTimeFormatter.format(issue.getUpdated()));
            else if (extraField.equals(CalendarServiceImpl.DESCRIPTION)) {
                if (StringUtils.isNotEmpty(issue.getDescription())) {
                    String renderedDescription = rendererManager.getRendererForType("atlassian-wiki-renderer").render(issue.getDescription(), null);
                    issueInfo.setDescription(renderedDescription);
                }
            }
        }
    }
}
