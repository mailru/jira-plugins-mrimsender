package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.user.ApplicationUser;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.Permission;
import ru.mail.jira.plugins.calendar.model.SubjectType;
import ru.mail.jira.plugins.calendar.rest.dto.PermissionItemDto;

import java.util.List;

@Transactional
public interface PermissionService {
    String getSubjectDisplayName(String subject, SubjectType subjectType);

    String getPermissionAvatar(Permission permission, SubjectType subjectType);

    boolean hasAdminPermission(ApplicationUser user, Calendar calendar);

    boolean hasUsePermission(ApplicationUser user, Calendar calendar);

    void removeCalendarPermissions(Calendar calendar);

    Permission getOrCreate(Calendar calendar, SubjectType subjectType, String subject);

    void addPermission(Calendar calendar, SubjectType subjectType, String subject, boolean canAdmin, boolean canUse);

    void updatePermissions(Calendar calendar, List<PermissionItemDto> permissions);
}
