package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.user.ApplicationUser;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.Permission;
import ru.mail.jira.plugins.calendar.model.PermissionType;
import ru.mail.jira.plugins.calendar.rest.dto.PermissionItemDto;

import java.util.List;

@Transactional
public interface PermissionService {

    String getPermissionAvatar(Permission permission, PermissionType permissionType);

    boolean hasAdminPermission(ApplicationUser user, Calendar calendar);

    boolean hasUsePermission(ApplicationUser user, Calendar calendar);

    void removeCalendarPermissions(Calendar calendar);

    Permission getOrCreate(Calendar calendar, PermissionType permissionType, String subject);

    void addPermission(Calendar calendar, PermissionType permissionType, String subject, boolean canAdmin, boolean canUse);

    void updatePermissions(Calendar calendar, List<PermissionItemDto> permissions);
}
