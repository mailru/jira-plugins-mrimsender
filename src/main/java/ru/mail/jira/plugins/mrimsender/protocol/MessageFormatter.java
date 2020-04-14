package ru.mail.jira.plugins.mrimsender.protocol;

import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.event.issue.MentionIssueEvent;
import com.atlassian.jira.event.type.EventType;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.fields.screen.FieldScreen;
import com.atlassian.jira.issue.fields.screen.FieldScreenManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenScheme;
import com.atlassian.jira.issue.fields.screen.issuetype.IssueTypeScreenSchemeManager;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.resolution.Resolution;
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
    private final ApplicationProperties applicationProperties;
    private final ConstantsManager constantsManager;
    private final DateTimeFormatter dateTimeFormatter;
    private final FieldManager fieldManager;
    private final IssueSecurityLevelManager issueSecurityLevelManager;
    private final I18nHelper i18nHelper;
    private final IssueTypeScreenSchemeManager issueTypeScreenSchemeManager;
    private final FieldScreenManager fieldScreenManager;


    public MessageFormatter(ApplicationProperties applicationProperties,
                            ConstantsManager constantsManager,
                            DateTimeFormatter dateTimeFormatter,
                            FieldManager fieldManager,
                            IssueSecurityLevelManager issueSecurityLevelManager,
                            I18nHelper i18nHelper,
                            IssueTypeScreenSchemeManager issueTypeScreenSchemeManager,
                            FieldScreenManager fieldScreenManager) {
        this.applicationProperties = applicationProperties;
        this.constantsManager = constantsManager;
        this.dateTimeFormatter = dateTimeFormatter;
        this.fieldManager = fieldManager;
        this.issueSecurityLevelManager = issueSecurityLevelManager;
        this.i18nHelper = i18nHelper;
        this.issueTypeScreenSchemeManager = issueTypeScreenSchemeManager;
        this.fieldScreenManager = fieldScreenManager;
    }

    private String formatUser(ApplicationUser user, String messageKey) {
        if (user != null)
            return user.getDisplayName() + " (" + user.getEmailAddress() + ")";
        else
            return i18nHelper.getText(messageKey);
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

    public String formatSystemFields(ApplicationUser recipient, Issue issue) {
        StringBuilder sb = new StringBuilder();

        if (issue.getIssueType() != null)
            appendField(sb, i18nHelper.getText("issue.field.issuetype"), issue.getIssueType().getNameTranslation(i18nHelper), false);

        appendField(sb, i18nHelper.getText("issue.field.affectsversions"), issue.getAffectedVersions());
        appendField(sb, i18nHelper.getText("issue.field.assignee"), formatUser(issue.getAssignee(), "common.concepts.unassigned"), false);
        appendField(sb, i18nHelper.getText("issue.field.attachment"), issue.getAttachments());
        appendField(sb, i18nHelper.getText("issue.field.components"), issue.getComponents());

        if (issue.getCreated() != null)
            appendField(sb, i18nHelper.getText("issue.field.created"), dateTimeFormatter.forUser(recipient).withStyle(DateTimeStyle.COMPLETE).format(issue.getCreated()), false);

        if (issue.getDueDate() != null)
            appendField(sb, i18nHelper.getText("issue.field.duedate"), dateTimeFormatter.forUser(recipient).withSystemZone().withStyle(DateTimeStyle.DATE).format(issue.getDueDate()), false);

        appendField(sb, i18nHelper.getText("issue.field.environment"), issue.getEnvironment(), false);
        appendField(sb, i18nHelper.getText("issue.field.fixversions"), issue.getFixVersions());
        appendField(sb, i18nHelper.getText("issue.field.labels"), issue.getLabels());
        appendField(sb, i18nHelper.getText("issue.field.priority"), formatPriority(issue.getPriority()), false);
        appendField(sb, i18nHelper.getText("issue.field.reporter"), formatUser(issue.getReporter(), "common.concepts.no.reporter"), false);

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

                    if (("Fix Version".equals(field) || "Component".equals(field) || "Version".equals(field))
                            && changeItem.get("oldvalue") != null && changeItem.get("newvalue") == null) {
                        newString = changeItem.getString("oldstring");
                        title = i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.deleted", title);
                    }

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

    public String formatEvent(ApplicationUser recipient, IssueEvent issueEvent) {
        Issue issue = issueEvent.getIssue();
        ApplicationUser user = issueEvent.getUser();
        String issueLink = String.format("%s/browse/%s", applicationProperties.getString(APKeys.JIRA_BASEURL), issue.getKey());

        StringBuilder sb = new StringBuilder();

        Long eventTypeId = issueEvent.getEventTypeId();
        if (EventType.ISSUE_CREATED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.created", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_UPDATED_ID.equals(eventTypeId) || EventType.ISSUE_COMMENT_DELETED_ID.equals(eventTypeId) || EventType.ISSUE_GENERICEVENT_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.updated", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.assigned", formatUser(user, "common.words.anonymous"), issueLink, formatUser(issue.getAssignee(), "common.concepts.unassigned")));
        } else if (EventType.ISSUE_RESOLVED_ID.equals(eventTypeId)) {
            Resolution resolution = issue.getResolution();
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.resolved", formatUser(user, "common.words.anonymous"), issueLink, resolution != null ? resolution.getNameTranslation(i18nHelper) : i18nHelper.getText("common.resolution.unresolved")));
        } else if (EventType.ISSUE_CLOSED_ID.equals(eventTypeId)) {
            Resolution resolution = issue.getResolution();
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.closed", formatUser(user, "common.words.anonymous"), issueLink, resolution != null ? resolution.getNameTranslation(i18nHelper) : i18nHelper.getText("common.resolution.unresolved")));
        } else if (EventType.ISSUE_COMMENTED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.commented", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_COMMENT_EDITED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.commentEdited", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_REOPENED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.reopened", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_DELETED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.deleted", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_MOVED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.moved", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_WORKLOGGED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.worklogged", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_WORKSTARTED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.workStarted", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_WORKSTOPPED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.workStopped", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_WORKLOG_UPDATED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.worklogUpdated", formatUser(user, "common.words.anonymous"), issueLink));
        } else if (EventType.ISSUE_WORKLOG_DELETED_ID.equals(eventTypeId)) {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.worklogDeleted", formatUser(user, "common.words.anonymous"), issueLink));
        } else {
            sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.updated", formatUser(user, "common.words.anonymous"), issueLink));
        }

        sb.append("\n").append(issue.getSummary());

        if (issueEvent.getWorklog() != null && !StringUtils.isBlank(issueEvent.getWorklog().getComment()))
            sb.append("\n\n").append(issueEvent.getWorklog().getComment());

        if (EventType.ISSUE_CREATED_ID.equals(eventTypeId))
            sb.append(formatSystemFields(recipient, issue));

        sb.append(formatChangeLog(issueEvent.getChangeLog(), EventType.ISSUE_ASSIGNED_ID.equals(eventTypeId)));

        if (issueEvent.getComment() != null && !StringUtils.isBlank(issueEvent.getComment().getBody()))
            sb.append("\n\n").append(issueEvent.getComment().getBody());

        return sb.toString();
    }

    public String formatEvent(MentionIssueEvent mentionIssueEvent) {
        Issue issue = mentionIssueEvent.getIssue();
        ApplicationUser user = mentionIssueEvent.getFromUser();
        String issueLink = String.format("%s/browse/%s", applicationProperties.getString(APKeys.JIRA_BASEURL), issue.getKey());

        StringBuilder sb = new StringBuilder();
        sb.append(i18nHelper.getText("ru.mail.jira.plugins.mrimsender.notification.mentioned", formatUser(user, "common.words.anonymous"), issueLink));
        sb.append("\n").append(issue.getSummary());

        if (!StringUtils.isBlank(mentionIssueEvent.getMentionText()))
            sb.append("\n\n").append(mentionIssueEvent.getMentionText());

        return sb.toString();
    }

    public String createIssueSummary(Issue issue, ApplicationUser user) {
        StringBuilder sb = new StringBuilder();
        sb.append(issue.getSummary()).append(" / ").append(issue.getKey());
        sb.append(formatSystemFields(user, issue));
        FieldScreenScheme fieldScreenScheme = issueTypeScreenSchemeManager.getFieldScreenScheme(issue);
        FieldScreen fieldScreen = fieldScreenScheme.getFieldScreen(IssueOperations.VIEW_ISSUE_OPERATION);

        fieldScreenManager
                .getFieldScreenTabs(fieldScreen)
                .forEach(tab -> fieldScreenManager
                        .getFieldScreenLayoutItems(tab)
                        .forEach(fieldScreenLayoutItem -> {
                            Field field = fieldManager.getField(fieldScreenLayoutItem.getFieldId());
                            if (fieldManager.isCustomField(field)) {
                                CustomField customField = (CustomField) field;
                                appendField(sb, customField.getFieldName(), customField.getValueFromIssue(issue), false);
                            }
                        })
                );

        return sb.toString();
    }
}
