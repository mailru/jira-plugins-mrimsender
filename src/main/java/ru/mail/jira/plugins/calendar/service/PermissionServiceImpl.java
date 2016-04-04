package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.Permission;
import ru.mail.jira.plugins.calendar.model.PermissionType;
import ru.mail.jira.plugins.calendar.rest.dto.PermissionItemDto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PermissionServiceImpl implements PermissionService {
    private final ActiveObjects ao;
    private final AvatarService avatarService;
    private final GlobalPermissionManager globalPermissionManager;
    private final GroupManager groupManager;
    private final PermissionManager permissionManager;
    private final ProjectManager projectManager;
    private final ProjectRoleManager projectRoleManager;
    private final UserManager userManager;

    public PermissionServiceImpl(ActiveObjects ao, AvatarService avatarService, GlobalPermissionManager globalPermissionManager, GroupManager groupManager, PermissionManager permissionManager, ProjectManager projectManager, ProjectRoleManager projectRoleManager, UserManager userManager) {
        this.ao = ao;
        this.avatarService = avatarService;
        this.globalPermissionManager = globalPermissionManager;
        this.groupManager = groupManager;
        this.permissionManager = permissionManager;
        this.projectManager = projectManager;
        this.projectRoleManager = projectRoleManager;
        this.userManager = userManager;
    }

    public String getPermissionAvatar(Permission permission, PermissionType permissionType) {
        switch (permissionType) {
            case USER:
                ApplicationUser user = userManager.getUserByKey(permission.getPermissionValue());
                if (user != null)
                    return getUserAvatarSrc(user);
                break;
            case PROJECT_ROLE:
                Long projectId = PermissionUtils.getProject(permission.getPermissionValue());
                if (projectId != null) {
                    Project project = projectManager.getProjectObj(projectId);
                    if (project != null)
                        return String.format("projectavatar?pid=%d&avatarId=%d&size=xxmall", projectId, project.getAvatar().getId());
                }
                break;
        }
        return "";
    }

    @Override
    public boolean hasAdminPermission(ApplicationUser user, Calendar calendar) {
        if (isAdministrator(user))
            return true;
        else {
            Permission[] permissions = calendar.getPermissions();
            for (Permission permission : permissions)
                if (permission.isAdmin()) {
                    String subject = permission.getPermissionValue();
                    switch (permission.getPermissionType()) {
                        case USER:
                            if (subject.equals(user.getKey()))
                                return true;
                            break;
                        case GROUP:
                            Group group = groupManager.getGroup(subject);
                            if (group != null && groupManager.isUserInGroup(user, group))
                                return true;
                            break;
                        case PROJECT_ROLE:
                            Long projectId = PermissionUtils.getProject(subject);
                            Long projectRoleId = PermissionUtils.getProjectRole(subject);
                            if (projectId == null)
                                continue;
                            Project project = projectManager.getProjectObj(projectId);
                            if (projectRoleId != null) {
                                ProjectRole projectRole = projectRoleManager.getProjectRole(projectRoleId);
                                if (projectRole != null && projectRoleManager.isUserInProjectRole(user, projectRole, project))
                                    return true;
                            } else if (permissionManager.hasPermission(Permissions.ADMINISTER, project, user, false))
                                return true;
                            break;
                    }
                }
            return false;
        }
    }

    @Override
    public boolean hasUsePermission(ApplicationUser user, Calendar calendar) {
        Permission[] permissions = calendar.getPermissions();
        for (Permission permission : permissions) {
            String subject = permission.getPermissionValue();
            switch (permission.getPermissionType()) {
                case USER:
                    if (subject.equals(user.getKey()))
                        return true;
                    break;
                case GROUP:
                    Group group = groupManager.getGroup(subject);
                    if (group != null && groupManager.isUserInGroup(user, group))
                        return true;
                    break;
                case PROJECT_ROLE:
                    Long projectId = PermissionUtils.getProject(subject);
                    Long projectRoleId = PermissionUtils.getProjectRole(subject);
                    if (projectId == null)
                        continue;
                    Project project = projectManager.getProjectObj(projectId);
                    if (projectRoleId != null) {
                        ProjectRole projectRole = projectRoleManager.getProjectRole(projectRoleId);
                        if (projectRole != null && projectRoleManager.isUserInProjectRole(user, projectRole, project))
                            return true;
                    } else if (permissionManager.hasPermission(Permissions.BROWSE, project, user, false))
                        return true;
                    break;
            }
        }
        return false;
    }

    @Override
    public void removeCalendarPermissions(Calendar calendar) {
        ao.delete(calendar.getPermissions());
    }

    @Override
    public void addPermission(Calendar calendar, PermissionType permissionType, String subject, boolean canAdmin, boolean canUse) {
        Permission permission = ao.create(Permission.class);
        permission.setAdmin(canAdmin);
        permission.setCalendar(calendar);
        permission.setPermissionType(permissionType);
        permission.setPermissionValue(subject);
        permission.save();
    }

    @Override
    public void updatePermissions(Calendar calendar, List<PermissionItemDto> permissions) {
        Map<Integer, Permission> toDelete = new HashMap<Integer, Permission>();
        for (Permission permission : calendar.getPermissions())
            toDelete.put(permission.getID(), permission);
        for (PermissionItemDto permissionDto : permissions) {
            Permission permission = getOrCreate(calendar, PermissionType.fromString(permissionDto.getType()), permissionDto.getId());
            permission.setAdmin("ADMIN".equals(permissionDto.getAccessType()));
            permission.save();

            toDelete.remove(permission.getID());
        }
        ao.delete(toDelete.values().toArray(new Permission[toDelete.size()]));
    }

    @Override
    public Permission getOrCreate(Calendar calendar, PermissionType permissionType, String permissionValue) {
        Permission[] permissions = ao.find(Permission.class, Query.select().where("CALENDAR_ID = ? AND PERMISSION_VALUE = ? AND PERMISSION_TYPE = ?", calendar.getID(), permissionValue, permissionType));
        if (permissions.length > 1)
            throw new ActiveObjectsException(String.format("Found more that one object of type Permission for calendar '%s' and permissionValue '%s'", calendar.getID(), permissionValue));
        else if (permissions.length == 0) {
            Permission permission = ao.create(Permission.class);
            permission.setCalendar(calendar);
            permission.setPermissionValue(permissionValue);
            permission.setPermissionType(permissionType);
            permission.save();
            return permission;
        } else
            return permissions[0];
    }

    private boolean isAdministrator(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }

    private String getUserAvatarSrc(ApplicationUser user) {
        return avatarService.getAvatarURL(user, userManager.getUserByKey(user.getKey()), Avatar.Size.SMALL).toString();
    }
}
