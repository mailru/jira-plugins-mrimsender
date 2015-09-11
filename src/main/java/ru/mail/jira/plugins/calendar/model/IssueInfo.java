package ru.mail.jira.plugins.calendar.model;

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
    private String envirounment;
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
    private Map<String,String> customFields = new LinkedHashMap<String, String>();

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

    public String getEnvirounment() {
        return envirounment;
    }

    public void setEnvirounment(String envirounment) {
        this.envirounment = envirounment;
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
}
