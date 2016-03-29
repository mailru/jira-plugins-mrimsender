package ru.mail.jira.plugins.calendar.rest.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class PermissionSubjectDto {
    @XmlElement
    private List<PermissionItemDto> users;
    @XmlElement
    private int usersCount;
    @XmlElement
    private List<PermissionItemDto> groups;
    @XmlElement
    private int groupsCount;
    @XmlElement
    private List<PermissionItemDto> projectRoles;
    @XmlElement
    private int projectRolesCount;

    public PermissionSubjectDto() {
    }

    public List<PermissionItemDto> getUsers() {
        return users;
    }

    public void setUsers(List<PermissionItemDto> users) {
        this.users = users;
    }

    public int getUsersCount() {
        return usersCount;
    }

    public void setUsersCount(int usersCount) {
        this.usersCount = usersCount;
    }

    public List<PermissionItemDto> getGroups() {
        return groups;
    }

    public void setGroups(List<PermissionItemDto> groups) {
        this.groups = groups;
    }

    public int getGroupsCount() {
        return groupsCount;
    }

    public void setGroupsCount(int groupsCount) {
        this.groupsCount = groupsCount;
    }

    public List<PermissionItemDto> getProjectRoles() {
        return projectRoles;
    }

    public void setProjectRoles(List<PermissionItemDto> projectRoles) {
        this.projectRoles = projectRoles;
    }

    public int getProjectRolesCount() {
        return projectRolesCount;
    }

    public void setProjectRolesCount(int projectRolesCount) {
        this.projectRolesCount = projectRolesCount;
    }
}
