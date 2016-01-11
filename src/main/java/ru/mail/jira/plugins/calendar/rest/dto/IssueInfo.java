package ru.mail.jira.plugins.calendar.rest.dto;

import com.atlassian.core.util.HTMLUtils;
import com.atlassian.jira.util.I18nHelper;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class IssueInfo {
    @XmlElement
    private String key;
    @XmlElement
    private String summary;
    @XmlElement
    private String type;
    @XmlElement
    private String description;
    @XmlElement
    private String assignee;
    @XmlElement
    private String reporter;
    @XmlElement
    private String status;
    @XmlElement
    private String statusColor;
    @XmlElement
    private String labels;
    @XmlElement
    private String components;
    @XmlElement
    private String dueDate;
    @XmlElement
    private String environment;
    @XmlElement
    private String priority;
    @XmlElement
    private String priorityIconUrl;
    @XmlElement
    private String resolution;
    @XmlElement
    private String affect;
    @XmlElement
    private String fixes;
    @XmlElement
    private String created;
    @XmlElement
    private String updated;
    @XmlElement
    private Map<String, String> customFields = new LinkedHashMap<String, String>();

    public IssueInfo(String key, String summary) {
        this.key = key;
        this.summary = summary;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatusColor() {
        return statusColor;
    }

    public void setStatusColor(String statusColor) {
        this.statusColor = statusColor;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public String getComponents() {
        return components;
    }

    public void setComponents(String components) {
        this.components = components;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPriorityIconUrl() {
        return priorityIconUrl;
    }

    public void setPriorityIconUrl(String priorityIconUrl) {
        this.priorityIconUrl = priorityIconUrl;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getAffect() {
        return affect;
    }

    public void setAffect(String affect) {
        this.affect = affect;
    }

    public String getFixes() {
        return fixes;
    }

    public void setFixes(String fixes) {
        this.fixes = fixes;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getUpdated() {
        return updated;
    }

    public void setUpdated(String updated) {
        this.updated = updated;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addCustomField(String name, String view) {
        this.customFields.put(name, view);
    }

    public String toFormatString(I18nHelper i18n) {
        StringBuilder str = new StringBuilder();
        if (StringUtils.isNotBlank(assignee))
            str.append(String.format("%s: %s\n", i18n.getText("issue.field.assignee"), HTMLUtils.stripTags(assignee).trim()));
        if (StringUtils.isNotBlank(reporter))
            str.append(String.format("%s: %s\n", i18n.getText("issue.field.reporter"), HTMLUtils.stripTags(reporter).trim()));
        if (StringUtils.isNotBlank(status))
            str.append(String.format("%s: %s\n", i18n.getText("common.words.status"), HTMLUtils.stripTags(status).trim()));
        if (StringUtils.isNotBlank(labels))
            str.append(String.format("%s: %s\n", i18n.getText("common.concepts.labels"), HTMLUtils.stripTags(labels).trim()));
        if (StringUtils.isNotBlank(components))
            str.append(String.format("%s: %s\n", i18n.getText("common.concepts.components"), HTMLUtils.stripTags(components).trim()));
        if (StringUtils.isNotBlank(dueDate))
            str.append(String.format("%s: %s\n", i18n.getText("issue.field.duedate"), HTMLUtils.stripTags(dueDate).trim()));
        if (StringUtils.isNotBlank(environment))
            str.append(String.format("%s: %s\n", i18n.getText("common.words.env"), HTMLUtils.stripTags(environment).trim()));
        if (StringUtils.isNotBlank(priority))
            str.append(String.format("%s: %s\n", i18n.getText("issue.field.priority"), HTMLUtils.stripTags(priority).trim()));
        if (StringUtils.isNotBlank(resolution))
            str.append(String.format("%s: %s\n", i18n.getText("issue.field.resolution"), HTMLUtils.stripTags(resolution).trim()));
        if (StringUtils.isNotBlank(affect))
            str.append(String.format("%s: %s\n", i18n.getText("issue.field.version"), HTMLUtils.stripTags(affect).trim()));
        if (StringUtils.isNotBlank(created))
            str.append(String.format("%s: %s\n", i18n.getText("issue.field.created"), HTMLUtils.stripTags(created).trim()));
        if (StringUtils.isNotBlank(updated))
            str.append(String.format("%s: %s\n", i18n.getText("issue.field.updated"), HTMLUtils.stripTags(updated).trim()));
        if (customFields != null)
            for (Map.Entry<String, String> entry : customFields.entrySet())
                str.append(String.format("%s: %s\n", entry.getKey(), HTMLUtils.stripTags(entry.getValue()).trim()));
        if (StringUtils.isNotBlank(description))
            str.append("\n").append(HTMLUtils.stripTags(description).trim());
        return str.toString().trim();
    }
}
