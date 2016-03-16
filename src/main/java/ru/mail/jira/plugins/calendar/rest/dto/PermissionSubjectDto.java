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
    private List<PermissionItemDto> groups;
    @XmlElement
    private List<PermissionItemDto> projectRoles;
    @XmlElement
    private boolean more = true;

    public PermissionSubjectDto(List<PermissionItemDto> users, List<PermissionItemDto> groups, List<PermissionItemDto> projectRoles) {
        this.users = users;
        this.groups = groups;
        this.projectRoles = projectRoles;
    }
}
