package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.security.IssueSecurityLevel;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.project.ProjectConstant;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import org.apache.commons.lang3.StringUtils;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import java.util.Collection;
import java.util.Iterator;

public class MessageFormatter {
    private final ConstantsManager constantsManager = ComponentAccessor.getConstantsManager();
    private final DateTimeFormatter dateTimeFormatter = ComponentAccessor.getComponentOfType(DateTimeFormatter.class);
    private final FieldManager fieldManager = ComponentAccessor.getFieldManager();
    private final IssueSecurityLevelManager issueSecurityLevelManager = ComponentAccessor.getIssueSecurityLevelManager();
    private final ApplicationUser recipient;
    private final I18nHelper i18nHelper;

    public MessageFormatter(ApplicationUser recipient) {
        this.i18nHelper = ComponentAccessor.getI18nHelperFactory().getInstance(recipient);
        this.recipient = recipient;
    }

    private String formatAssignee(User assignee) {
        if (assignee != null)
            return assignee.getDisplayName();
        else
            return i18nHelper.getText("common.concepts.unassigned");
    }

    private String formatReporter(User reporter) {
        if (reporter != null)
            return reporter.getDisplayName();
        else
            return i18nHelper.getText("common.concepts.no.reporter");
    }

    private String formatPriority(Priority priority) {
        if (priority != null && !priority.getId().equals(constantsManager.getDefaultPriorityObject().getId()))
            return priority.getNameTranslation(i18nHelper);
        else
            return null;
    }

    private void appendField(StringBuilder sb, String title, String value, boolean appendEmpty) {
        if (appendEmpty || !StringUtils.isBlank(value)) {
            if (sb.length() == 0)
                sb.append("\n");
            sb.append("\n").append(title).append(": ").append(StringUtils.defaultString(value));
        }
    }

    private void appendField(StringBuilder sb, String title, Collection<?> collection) {
        if (collection != null) {
            StringBuilder value = new StringBuilder();
            Iterator<?> iterator = collection.iterator();
            while (iterator.hasNext()) {
                Object object = iterator.next();
                if (object instanceof ProjectConstant)
                    value.append(((ProjectConstant) object).getName());
                if (object instanceof Attachment)
                    value.append(((Attachment) object).getFilename());
                if (object instanceof Label)
                    value.append(((Label) object).getLabel());
                if (iterator.hasNext())
                    value.append(", ");
            }
            appendField(sb, title, value.toString(), false);
        }
    }

    public String formatSystemFields(Issue issue) {
        StringBuilder sb = new StringBuilder();

        if (issue.getIssueTypeObject() != null)
            appendField(sb, i18nHelper.getText("issue.field.issuetype"), issue.getIssueTypeObject().getNameTranslation(i18nHelper), false);

        appendField(sb, i18nHelper.getText("issue.field.affectsversions"), issue.getAffectedVersions());
        appendField(sb, i18nHelper.getText("issue.field.assignee"), formatAssignee(issue.getAssignee()), false);
        appendField(sb, i18nHelper.getText("issue.field.attachment"), issue.getAttachments());
        appendField(sb, i18nHelper.getText("issue.field.components"), issue.getComponentObjects());

        if (issue.getCreated() != null)
            appendField(sb, i18nHelper.getText("issue.field.created"), dateTimeFormatter.forUser(recipient.getDirectoryUser()).withStyle(DateTimeStyle.COMPLETE).format(issue.getCreated()), false);

        if (issue.getDueDate() != null)
            appendField(sb, i18nHelper.getText("issue.field.duedate"), dateTimeFormatter.forUser(recipient.getDirectoryUser()).withSystemZone().withStyle(DateTimeStyle.DATE).format(issue.getDueDate()), false);

        appendField(sb, i18nHelper.getText("issue.field.environment"), issue.getEnvironment(), false);
        appendField(sb, i18nHelper.getText("issue.field.fixversions"), issue.getFixVersions());
        appendField(sb, i18nHelper.getText("issue.field.labels"), issue.getLabels());
        appendField(sb, i18nHelper.getText("issue.field.priority"), formatPriority(issue.getPriorityObject()), false);
        appendField(sb, i18nHelper.getText("issue.field.reporter"), formatReporter(issue.getReporter()), false);

        if (issue.getSecurityLevelId() != null) {
            IssueSecurityLevel issueSecurityLevel = issueSecurityLevelManager.getSecurityLevel(issue.getSecurityLevelId());
            String value = issueSecurityLevel.getName();
            if (!StringUtils.isBlank(issueSecurityLevel.getDescription()))
                value += " " + issueSecurityLevel.getDescription();
            appendField(sb, i18nHelper.getText("issue.field.securitylevel"), value, false);
        }

        if (!StringUtils.isBlank(issue.getDescription()))
            sb.append("\n\n").append(issue.getDescription());

        return sb.toString();
    }

    private String formatChangeLog(GenericValue changeLog, boolean ignoreAssigneeField) {
        StringBuilder sb = new StringBuilder();
        if (changeLog != null)
            try {
                String changedDescription = null;

                for (GenericValue changeItem : changeLog.getRelated("ChildChangeItem")) {
                    String field = StringUtils.defaultString(changeItem.getString("field"));
                    String newString = StringUtils.defaultString(changeItem.getString("newstring"));

                    if ("description".equals(field)) {
                        changedDescription = newString;
                        continue;
                    }
                    if ("WorklogTimeSpent".equals(field) || "WorklogId".equals(field) || "assignee".equals(field) && ignoreAssigneeField)
                        continue;

                    String title = field;
                    if (!"custom".equalsIgnoreCase(changeItem.getString("fieldtype")))
                        title = i18nHelper.getText("issue.field." + field.replaceAll(" ", "").toLowerCase());

                    if (fieldManager.isNavigableField(field)) {
                        final NavigableField navigableField = fieldManager.getNavigableField(field);
                        if (navigableField != null)
                            newString = navigableField.prettyPrintChangeHistory(newString, i18nHelper);
                    }

                    appendField(sb, title, newString, true);
                }

                if (!StringUtils.isBlank(changedDescription))
                    sb.append("\n\n").append(changedDescription);
            } catch (GenericEntityException ignored) {
            }
        return sb.toString();
    }

    public String formatIssueEvent(IssueEvent issueEvent) {
        Issue issue = issueEvent.getIssue();
        User user = issueEvent.getUser();
        String issueLink = String.format("%s/browse/%s", ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL), issue.getKey());

        StringBuilder sb = new StringBuilder();

        Long eventTypeId = issueEvent.getEventTypeId();
        if (EventType.ISSUE_CREATED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.created", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_UPDATED_ID.equals(eventTypeId) || EventType.ISSUE_COMMENT_DELETED_ID.equals(eventTypeId) || EventType.ISSUE_GENERICEVENT_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.updated", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.assigned", user.getDisplayName(), issueLink, formatAssignee(issue.getAssignee())));
        } else if (EventType.ISSUE_RESOLVED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.resolved", user.getDisplayName(), issueLink, issue.getResolutionObject().getNameTranslation(i18nHelper)));
        } else if (EventType.ISSUE_CLOSED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.closed", user.getDisplayName(), issueLink, issue.getResolutionObject().getNameTranslation(i18nHelper)));
        } else if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.commented", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_COMMENT_EDITED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.commentEdited", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_REOPENED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.reopened", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_DELETED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.deleted", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_MOVED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.moved", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_WORKLOGGED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.worklogged", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_WORKSTARTED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.workStarted", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_WORKSTOPPED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.workStopped", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_WORKLOG_UPDATED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.worklogUpdated", user.getDisplayName(), issueLink));
        } else if (EventType.ISSUE_WORKLOG_DELETED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.worklogDeleted", user.getDisplayName(), issueLink));
        } else
            throw new IllegalArgumentException(String.format("Unknown issue event type (%d)", eventTypeId));

        sb.append("\n").append(issue.getSummary());

        if (issueEvent.getWorklog() != null && !StringUtils.isBlank(issueEvent.getWorklog().getComment()))
            sb.append("\n\n").append(issueEvent.getWorklog().getComment());

        if (EventType.ISSUE_CREATED_ID.equals(eventTypeId))
            sb.append(formatSystemFields(issue));

        sb.append(formatChangeLog(issueEvent.getChangeLog(), EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)));

        if (issueEvent.getComment() != null && !StringUtils.isBlank(issueEvent.getComment().getBody()))
            sb.append("\n\n").append(issueEvent.getComment().getBody());

        return sb.toString();
    }
}
