package ru.mail.jira.plugins.calendar.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class PermissionItemDto {
    @XmlElement
    private String id;
    @XmlElement
    private String text;
    @XmlElement
    private String project;
    @XmlElement
    private String projectRole;
    @XmlElement
    private String type;
    @XmlElement
    private String accessType;
    @XmlElement
    private String avatarUrl;

    public PermissionItemDto() {

    }

    public PermissionItemDto(String id, String text, String type, String accessType, String avatarUrl) {
        this.id = id;
        this.text = text;
        this.type = type;
        this.accessType = accessType;
        this.avatarUrl = avatarUrl;
    }

    public PermissionItemDto(String id, String project, String projectRole, String type, String accessType, String avatarUrl) {
        this.id = id;
        this.project = project;
        this.projectRole = projectRole;
        this.type = type;
        this.accessType = accessType;
        this.avatarUrl = avatarUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getProjectRole() {
        return projectRole;
    }

    public void setProjectRole(String projectRole) {
        this.projectRole = projectRole;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
