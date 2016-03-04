package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.UserCalendar;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.calendar.rest.dto.UserDataDto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UserDataService {
    private final static Logger log = LoggerFactory.getLogger(UserDataService.class);

    private ActiveObjects ao;
    private CalendarService calendarService;
    private GlobalPermissionManager globalPermissionManager;
    private UserCalendarService userCalendarService;

    public void setAo(ActiveObjects ao) {
        this.ao = ao;
    }

    public void setGlobalPermissionManager(GlobalPermissionManager globalPermissionManager) {
        this.globalPermissionManager = globalPermissionManager;
    }

    public void setCalendarService(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    public void setUserCalendarService(UserCalendarService userCalendarService) {
        this.userCalendarService = userCalendarService;
    }

    public UserDataDto getUserDataDto(ApplicationUser user) {
        return getUserDataDto(user, getUserData(user));
    }

    public UserDataDto getUserDataDto(ApplicationUser user, UserData userData) {
        UserDataDto userDataDto = new UserDataDto(userData);
        if (isAdministrator(user))
            userDataDto.setLastLikeFlagShown(userData.getLastLikeFlagShown());
        return userDataDto;
    }

    public UserData getUserData(final ApplicationUser user) {
        return getUserData(user.getKey());
    }

    private UserData getUserData(final String userKey) {
        return ao.executeInTransaction(new TransactionCallback<UserData>() {
            @Override
            public UserData doInTransaction() {
                UserData[] userDatas = ao.find(UserData.class, Query.select().where("USER_KEY = ?", userKey));
                UserData userData;
                if (userDatas.length == 0) {
                    userData = ao.create(UserData.class);
                    userData.setUserKey(userKey);
                    userData.setHideWeekends(false);
                } else
                    userData = userDatas[0];

                if (userData.getIcalUid() == null) {
                    userData.setICalUid(UUID.randomUUID().toString().substring(0, 8));
                    userData.save();
                }
                return userData;
            }
        });
    }

    public UserDataDto updateUserData(final ApplicationUser user, final UserDataDto userDataDto) {
        return ao.executeInTransaction(new TransactionCallback<UserDataDto>() {
            @Override
            public UserDataDto doInTransaction() {
                UserData userData = getUserData(user);
                if (userDataDto.getCalendarView() != null)
                    userData.setDefaultView(userDataDto.getCalendarView());
                userData.setHideWeekends(userDataDto.isHideWeekends());
                userData.save();
                if (userDataDto.getCalendars() != null && !userDataDto.getCalendars().isEmpty())
                    for (Integer calendarId : userDataDto.getCalendars())
                        try {
                            Calendar calendar = calendarService.getCalendar(calendarId);
                            userCalendarService.addCalendarToUser(userData.getUserKey(), calendar, true);
                        } catch (GetException e) {
                            log.warn("User attempt to add not existing calendar. User={}, calendar={}", user.getKey(), calendarId);
                        }
                return getUserDataDto(user, userData);
            }
        });
    }

    public UserData getUserDataByIcalUid(final String icalUid) {
        return ao.executeInTransaction(new TransactionCallback<UserData>() {
            @Override
            public UserData doInTransaction() {
                UserData[] userDatas = ao.find(UserData.class, Query.select().where("ICAL_UID = ?", icalUid));
                if (userDatas.length == 0)
                    return null;
                else if (userDatas.length == 1)
                    return userDatas[0];
                else
                    throw new ActiveObjectsException("Found more that one object of type UserData for uid" + icalUid);
            }
        });
    }

    public UserDataDto updateIcalUid(final ApplicationUser user) {
        return ao.executeInTransaction(new TransactionCallback<UserDataDto>() {
            @Override
            public UserDataDto doInTransaction() {
                UserData userData = getUserData(user);
                userData.setICalUid(UUID.randomUUID().toString().substring(0, 8));
                userData.save();
                return getUserDataDto(user, userData);
            }
        });
    }

    public void updateUserLastLikeFlagShown(final ApplicationUser user) {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                UserData userData = getUserData(user);
                userData.setLastLikeFlagShown(System.currentTimeMillis());
                userData.save();
                return null;
            }
        });
    }

    public void removeUserCalendar(final ApplicationUser user, final Integer calendarId) {
        userCalendarService.removeCalendar(user.getKey(), calendarId);
    }

    private boolean isAdministrator(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }
}
