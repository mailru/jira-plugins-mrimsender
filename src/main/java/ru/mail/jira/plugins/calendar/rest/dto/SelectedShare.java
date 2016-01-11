package ru.mail.jira.plugins.calendar.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class SelectedShare {
    @XmlElement
    private String group;
    @XmlElement
    private long projectId;
    @XmlElement
    private String projectName;
    @XmlElement
    private long roleId;
    @XmlElement
    private String roleName;
    @XmlElement
    private String error;

    public SelectedShare() { }

    public SelectedShare(String group) {
        this.group = group;
    }

    public SelectedShare(long projectId, String projectName, long roleId, String roleName) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.roleId = roleId;
        this.roleName = roleName;
    }

    public void setError(String error) {
        this.error = error;
    }

    public long getProjectId() {
        return projectId;
    }
}
