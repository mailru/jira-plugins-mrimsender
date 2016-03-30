package ru.mail.jira.plugins.calendar.rest.dto;

import ru.mail.jira.plugins.calendar.model.PermissionType;

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
    private String type;
    @XmlElement
    private String accessType;
    @XmlElement
    private String avatarUrl;
    @XmlElement
    private boolean available;

    @XmlElement
    private String userDisplayName;
    @XmlElement
    private String userEmail;
    @XmlElement
    private String userName;

    @XmlElement
    private String project;
    @XmlElement
    private String projectRole;

    public PermissionItemDto() {

    }

    private PermissionItemDto(String id, String type, String accessType, String avatarUrl) {
        this.id = id;
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


    public static PermissionItemDto buildUserDto(String id, String displayName, String email, String name, String accessType, String avatarUrl) {
        PermissionItemDto itemDto = new PermissionItemDto(id, PermissionType.USER.name(), accessType, avatarUrl);
        itemDto.userDisplayName = displayName;
        itemDto.userEmail = email;
        itemDto.userName = name;
        return itemDto;
    }

    public static PermissionItemDto buildGroupDto(String id, String text, String accessType) {
        PermissionItemDto itemDto = new PermissionItemDto(id, PermissionType.GROUP.name(), accessType, "");
        itemDto.setText(text);
        return itemDto;
    }

    public static PermissionItemDto buildProjectRoleDto(String id, String project, String projectRole, String accessType, String avatarUrl) {
        PermissionItemDto itemDto = new PermissionItemDto(id, PermissionType.PROJECT_ROLE.name(), accessType, avatarUrl);
        itemDto.project = project;
        itemDto.projectRole = projectRole;
        return itemDto;
    }
}
