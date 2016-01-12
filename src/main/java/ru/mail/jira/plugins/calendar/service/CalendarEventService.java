package ru.mail.jira.plugins.calendar.service;

import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.RendererManager;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class CalendarEventService {
    private final static Logger log = LoggerFactory.getLogger(CalendarEventService.class);

    private final static int MILLIS_IN_DAY = 86400000;
    private final static String DATE_RANGE_FORMAT = "yyyy-MM-dd";

    public static final String CREATED_DATE_KEY = "created";
    public static final String UPDATED_DATE_KEY = "updated";
    public static final String RESOLVED_DATE_KEY = "resolved";
    public static final String DUE_DATE_KEY = "due_date";

    private final CalendarService calendarService;

    private final ApplicationProperties applicationProperties;
    private final CustomFieldManager customFieldManager;
    private final DateTimeFormatter dateTimeFormatter;
    private final FieldLayoutManager fieldLayoutManager;
    private final IssueService issueService;
    private final RendererManager rendererManager;
    private final SearchRequestService searchRequestService;
    private final SearchProvider searchProvider;

    public CalendarEventService(CalendarService calendarService,
                                ApplicationProperties applicationProperties,
                                CustomFieldManager customFieldManager,
                                DateTimeFormatter dateTimeFormatter,
                                FieldLayoutManager fieldLayoutManager,
                                IssueService issueService,
                                RendererManager rendererManager,
                                SearchRequestService searchRequestService,
                                SearchProvider searchProvider) {
        this.calendarService = calendarService;
        this.applicationProperties = applicationProperties;
        this.customFieldManager = customFieldManager;
        this.dateTimeFormatter = dateTimeFormatter;
        this.fieldLayoutManager = fieldLayoutManager;
        this.issueService = issueService;
        this.rendererManager = rendererManager;
        this.searchRequestService = searchRequestService;
        this.searchProvider = searchProvider;
    }

    public List<Event> findEvents(final int calendarId,
                                  final String start,
                                  final String end,
                                  final ApplicationUser user) throws ParseException, SearchException {
        return findEvents(calendarId, start, end, user, false);
    }

    public List<Event> findEvents(final int calendarId,
                                  final String start,
                                  final String end,
                                  final ApplicationUser user,
                                  final boolean includeIssueInfo) throws ParseException, SearchException {
        Calendar calendarModel = calendarService.getCalendar(calendarId);
        DateFormat dateFormat = new SimpleDateFormat(DATE_RANGE_FORMAT);
        String source = calendarModel.getSource();
        if (source.startsWith("project_"))
            return getProjectEvents(calendarModel, Long.parseLong(source.substring("project_".length())),
                                    calendarModel.getEventStart(), calendarModel.getEventEnd(), dateFormat.parse(start), dateFormat.parse(end), user, includeIssueInfo);
        else if (source.startsWith("filter_"))
            return getFilterEvents(calendarModel, Long.parseLong(source.substring("filter_".length())),
                                   calendarModel.getEventStart(), calendarModel.getEventEnd(), dateFormat.parse(start), dateFormat.parse(end), user, includeIssueInfo);
        else {
            return Collections.emptyList();
        }
    }

    public IssueInfo getEventInfo(ApplicationUser user, int calendarId, String eventId) {
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
        JiraServiceContext jsCtx = new JiraServiceContextImpl(user);
        SearchRequest filter = searchRequestService.getFilter(jsCtx, filterId);

        if (filter == null) {
            log.error("Filter with id => " + filterId + " is null. Maybe it was deleted");
            return new ArrayList<Event>(0);
        }

        JqlClauseBuilder jqlBuilder = JqlQueryBuilder.newClauseBuilder(filter.getQuery());
        return getEvents(calendar, jqlBuilder, startField, endField, startTime, endTime, user, includeIssueInfo);
    }

    private List<Event> getEvents(Calendar calendar,
                                  JqlClauseBuilder jqlBuilder,
                                  String startField,
                                  String endField,
                                  Date startTime,
                                  Date endTime,
                                  ApplicationUser user, boolean includeIssueInfo) throws SearchException {
        List<Event> result = new ArrayList<Event>();
        SimpleDateFormat clientDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

        CustomField startCF = null;
        if (startField.startsWith("customfield_")) {
            startCF = customFieldManager.getCustomFieldObject(startField);
            if (startCF == null)
                throw new IllegalArgumentException("Bad custom field id => " + startField);
        }

        CustomField endCF = null;
        if (StringUtils.isNotEmpty(endField) && endField.startsWith("customfield_")) {
            endCF = customFieldManager.getCustomFieldObject(endField);
            if (endCF == null)
                throw new IllegalArgumentException("Bad custom field id => " + endField);
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        jqlBuilder.and().sub();
        addDateCondition(startField, startTime, endTime, jqlBuilder, simpleDateFormat, false);
        if (StringUtils.isNotEmpty(endField)) {
            jqlBuilder.or();
            addDateCondition(endField, startTime, endTime, jqlBuilder, simpleDateFormat, true);
        }
        jqlBuilder.endsub();
        boolean dateFieldsIsDraggable = isDateFieldsDraggable(startField, endField);

        List<Issue> issues = searchProvider.search(jqlBuilder.buildQuery(), user, PagerFilter.getUnlimitedFilter()).getIssues();
        for (Issue issue : issues) {
            try {
                Date startDate = startCF == null ? retrieveDateByField(issue, startField) : retrieveDateByField(issue, startCF);
                Date endDate = null;
                if (StringUtils.isNotEmpty(endField))
                    endDate = endCF == null ? retrieveDateByField(issue, endField) : retrieveDateByField(issue, endCF);

                boolean isAllDay = isAllDayEvent(startCF, endCF, startField, endField);

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

                if (startDate != null) {
                    event.setStart(clientDateFormat.format(startDate));
                    if (endDate != null)
                        event.setEnd(isAllDay ? clientDateFormat.format(new Date(endDate.getTime() + MILLIS_IN_DAY)) : clientDateFormat.format(endDate));
                } else {
                    event.setStart(clientDateFormat.format(endDate));
                }

                event.setStartEditable(dateFieldsIsDraggable && issueService.isEditable(issue, ApplicationUsers.toDirectoryUser(user)));
                event.setDurationEditable(isDateFieldDraggable(endField) && startDate != null && endDate != null && issueService.isEditable(issue, ApplicationUsers.toDirectoryUser(user)));

                if(includeIssueInfo)
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

    private void addDateCondition(String field, Date startTime, Date endTime, JqlClauseBuilder jcb, SimpleDateFormat simpleDateFormat, boolean nullable) {
        if (field.equals(DUE_DATE_KEY))
            jcb.dueBetween(simpleDateFormat.format(startTime), simpleDateFormat.format(endTime));
        else if (field.equals(CREATED_DATE_KEY))
            jcb.createdBetween(simpleDateFormat.format(startTime), simpleDateFormat.format(endTime));
        else if (field.equals(UPDATED_DATE_KEY))
            jcb.updatedBetween(simpleDateFormat.format(startTime), simpleDateFormat.format(endTime));
        else if (field.equals(RESOLVED_DATE_KEY))
            jcb.updatedBetween(simpleDateFormat.format(startTime), simpleDateFormat.format(endTime));
        else if (field.startsWith("customfield_")) {
            CustomField customField = customFieldManager.getCustomFieldObject(field);
            if (customField == null)
                throw new IllegalArgumentException("Bad custom field id => " + field);
            jcb.addDateRangeCondition("cf[" + customField.getIdAsLong() + "]", startTime, endTime);
        } else
            throw new IllegalArgumentException("Bad field => " + field);
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

    public void dragEvent(ApplicationUser user, Calendar calendar, Issue issue, int dayDelta, int millisDelta) throws Exception {
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
        if (calendar.getEventEnd() != null) {
            if (calendar.getEventEnd().startsWith("customfield_")) {
                eventEndCF = customFieldManager.getCustomFieldObject(calendar.getEventEnd());
                if (eventEndCF == null)
                    throw new IllegalStateException("Can not find custom field => " + calendar.getEventStart()); //todo:xxx may be not stateException
                eventEndCFValue = (Timestamp) issue.getCustomFieldValue(eventEndCF);
            } else
                eventEndIsDueDate = true;
        }

        IssueInputParameters issueInputParams = issueService.newIssueInputParameters();

        String dateFormat = applicationProperties.getDefaultBackedString(APKeys.JIRA_DATE_PICKER_JAVA_FORMAT);
        String dateTimeFormat = applicationProperties.getDefaultBackedString(APKeys.JIRA_DATE_TIME_PICKER_JAVA_FORMAT);
        Locale locale = ComponentAccessor.getI18nHelperFactory().getInstance(user).getLocale();

        if (eventStartIsDueDate) {
            Timestamp newDueDate = getNewTimestamp(issue.getDueDate(), dayDelta, millisDelta);
            issueInputParams.setDueDate(new SimpleDateFormat(dateFormat, locale).format(newDueDate));
        } else if (eventStartCFValue != null) {
            Timestamp value = getNewTimestamp(eventStartCFValue, dayDelta, millisDelta);
            String keyForDateFormat = eventStartCF.getCustomFieldType() instanceof DateTimeCFType ? dateTimeFormat : dateFormat;
            issueInputParams.addCustomFieldValue(eventStartCF.getIdAsLong(), new SimpleDateFormat(keyForDateFormat, locale).format(value));
        }

        if (eventEndIsDueDate) {
            Timestamp newDueDate = getNewTimestamp(issue.getDueDate(), dayDelta, millisDelta);
            issueInputParams.setDueDate(new SimpleDateFormat(dateFormat, locale).format(newDueDate));
        } else if (eventEndCF != null && eventEndCFValue != null) {
            Timestamp value = getNewTimestamp(eventEndCFValue, dayDelta, millisDelta);
            String keyForDateFormat = eventEndCF.getCustomFieldType() instanceof DateTimeCFType ? dateTimeFormat : dateFormat;
            issueInputParams.addCustomFieldValue(eventEndCF.getIdAsLong(), new SimpleDateFormat(keyForDateFormat, locale).format(value));
        }

        IssueService.UpdateValidationResult updateValidationResult = issueService.validateUpdate(ApplicationUsers.toDirectoryUser(user), issue.getId(), issueInputParams);
        if (!updateValidationResult.isValid())
            throw new Exception(CommonUtils.formatErrorCollection(updateValidationResult.getErrorCollection()));

        IssueService.IssueResult updateResult = issueService.update(ApplicationUsers.toDirectoryUser(user), updateValidationResult);
        if (!updateResult.isValid())
            throw new Exception(CommonUtils.formatErrorCollection(updateResult.getErrorCollection()));
    }

    private Timestamp getNewTimestamp(Date source, int dayDelta, int millisDelta) {
        int summaryMillis = MILLIS_IN_DAY * dayDelta + millisDelta;
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTime(source);
        gregorianCalendar.add(java.util.Calendar.MILLISECOND, summaryMillis);
        return new Timestamp(gregorianCalendar.getTimeInMillis());
    }

    public void resizeEvent(ApplicationUser user, Calendar calendar, Issue issue, int dayDelta, int millisDelta) throws Exception {
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
        if (calendar.getEventEnd() != null) {
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

        IssueInputParameters issueInputParams = issueService.newIssueInputParameters();

        String dateFormat = applicationProperties.getDefaultBackedString(APKeys.JIRA_DATE_PICKER_JAVA_FORMAT);
        String dateTimeFormat = applicationProperties.getDefaultBackedString(APKeys.JIRA_DATE_TIME_PICKER_JAVA_FORMAT);
        Locale locale = ComponentAccessor.getI18nHelperFactory().getInstance(user).getLocale();

        if (eventEndIsDueDate) {
            Timestamp newDueDate = getNewTimestamp(issue.getDueDate(), dayDelta, millisDelta);
            issueInputParams.setDueDate(new SimpleDateFormat(dateFormat, locale).format(newDueDate));
        } else {
            String keyForDateFormat = eventEndCF.getCustomFieldType() instanceof DateTimeCFType ? dateTimeFormat : dateFormat;
            Timestamp value = getNewTimestamp(eventEndDateValue, dayDelta, millisDelta);
            issueInputParams.addCustomFieldValue(eventEndCF.getIdAsLong(), new SimpleDateFormat(keyForDateFormat, locale).format(value));
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

    private boolean isDateFieldDraggable(String field) {
        return !CREATED_DATE_KEY.equals(field) && !UPDATED_DATE_KEY.equals(field) && !RESOLVED_DATE_KEY.equals(field);
    }

    private boolean isDateFieldNotDraggable(String field) {
        return !isDateFieldDraggable(field);
    }

    private boolean isDateFieldsDraggable(String startField, String endField) {
        return isDateFieldDraggable(startField) && isDateFieldDraggable(endField);
    }

    private boolean isDateFieldsNotDraggable(String startField, String endField) {
        return !isDateFieldsDraggable(startField, endField);
    }

    private void fillDisplayedFields(IssueInfo issueInfo, String[] extraFields, Issue issue) {
        for (String extraField : extraFields) {
            if (extraField.startsWith("customfield_")) {
                CustomField customField = customFieldManager.getCustomFieldObject(extraField);
                FieldLayoutItem fieldLayoutItem = fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem(customField);
                String columnViewHtml = customField.getColumnViewHtml(fieldLayoutItem, new HashMap(), issue);
                if (StringUtils.isNotEmpty(columnViewHtml))
                    issueInfo.addCustomField(customField.getName(), columnViewHtml);
            } else if (extraField.equals(CalendarService.REPORTER)) {
                if (issue.getReporter() != null) {
                    FieldLayoutItem reporterLayoutItem = fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem("reporter");


                    String columnViewHtml = ((ReporterSystemField) reporterLayoutItem.getOrderableField()).getColumnViewHtml(reporterLayoutItem, new HashMap(), issue);
                    issueInfo.setReporter(columnViewHtml);
                }
            } else if (extraField.equals(CalendarService.ASSIGNEE)) {
                if (issue.getAssignee() != null) {
                    FieldLayoutItem assigneeLayoutItem = fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem("assignee");
                    String columnViewHtml = ((AssigneeSystemField) assigneeLayoutItem.getOrderableField()).getColumnViewHtml(assigneeLayoutItem, new HashMap(), issue);
                    issueInfo.setAssignee(columnViewHtml);
                }
            } else if (extraField.equals(CalendarService.STATUS))
                issueInfo.setStatus(issue.getStatusObject().getName());
            else if (extraField.equals(CalendarService.LABELS)) {
                if (issue.getLabels() != null && !issue.getLabels().isEmpty()) {
                    FieldLayoutItem labelsLayoutItem = fieldLayoutManager.getFieldLayout(issue).getFieldLayoutItem("labels");
                    String columnViewHtml = ((LabelsSystemField) labelsLayoutItem.getOrderableField()).getColumnViewHtml(labelsLayoutItem, new HashMap(), issue);
                    issueInfo.setLabels(columnViewHtml);
                }
            } else if (extraField.equals(CalendarService.COMPONENTS) && issue.getComponentObjects() != null && !issue.getComponentObjects().isEmpty()) {
                List<String> components = new ArrayList<String>();
                for (ProjectComponent pc : issue.getComponentObjects())
                    components.add(pc.getName());
                issueInfo.setComponents(components.toString());
            } else if (extraField.equals(CalendarService.DUEDATE) && issue.getDueDate() != null)
                issueInfo.setDueDate(dateTimeFormatter.forLoggedInUser().format(issue.getDueDate()));
            else if (extraField.equals(CalendarService.ENVIRONMENT) && issue.getEnvironment() != null)
                issueInfo.setEnvironment(issue.getEnvironment());
            else if (extraField.equals(CalendarService.PRIORITY) && issue.getPriorityObject() != null) {
                issueInfo.setPriority(issue.getPriorityObject().getName());
                issueInfo.setPriorityIconUrl(issue.getPriorityObject().getIconUrl());
            } else if (extraField.equals(CalendarService.RESOLUTION) && issue.getResolutionObject() != null)
                issueInfo.setResolution(issue.getResolutionObject().getName());
            else if (extraField.equals(CalendarService.AFFECT) && issue.getAffectedVersions() != null && !issue.getAffectedVersions().isEmpty()) {
                List<String> affectVersions = new ArrayList<String>();
                for (Version ver : issue.getAffectedVersions())
                    affectVersions.add(ver.getName());
                issueInfo.setAffect(affectVersions.toString());
            } else if (extraField.equals(CalendarService.CREATED))
                issueInfo.setCreated(dateTimeFormatter.forLoggedInUser().format(issue.getCreated()));
            else if (extraField.equals(CalendarService.UPDATED))
                issueInfo.setUpdated(dateTimeFormatter.forLoggedInUser().format(issue.getUpdated()));
            else if (extraField.equals(CalendarService.DESCRIPTION)) {
                if (StringUtils.isNotEmpty(issue.getDescription())) {
                    String renderedDescription = rendererManager.getRendererForType("atlassian-wiki-renderer").render(issue.getDescription(), null);
                    issueInfo.setDescription(renderedDescription);
                }
            }
        }
    }
}
