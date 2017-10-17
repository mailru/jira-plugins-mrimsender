package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.timezone.TimeZoneManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.calendar.rest.dto.UserDataDto;

import java.util.UUID;

@Component
public class UserDataService {
    private final static Logger log = LoggerFactory.getLogger(UserDataService.class);

    private final ActiveObjects ao;
    private final AvatarService avatarService;
    private final CalendarService calendarService;
    private final GlobalPermissionManager globalPermissionManager;
    private final UserCalendarService userCalendarService;
    private final TimeZoneManager timeZoneManager;

    @Autowired
    public UserDataService(
            @ComponentImport AvatarService avatarService,
            @ComponentImport GlobalPermissionManager globalPermissionManager,
            @ComponentImport ActiveObjects ao,
            CalendarService calendarService,
            UserCalendarService userCalendarService,
            @ComponentImport TimeZoneManager timeZoneManager) {
        this.ao = ao;
        this.avatarService = avatarService;
        this.calendarService = calendarService;
        this.globalPermissionManager = globalPermissionManager;
        this.userCalendarService = userCalendarService;
        this.timeZoneManager = timeZoneManager;
    }

    public UserDataDto getUserDataDto(ApplicationUser user) {
        return getUserDataDto(user, getUserData(user));
    }

    public UserDataDto getUserDataDto(ApplicationUser user, UserData userData) {
        UserDataDto userDataDto = new UserDataDto(userData);
        userDataDto.setName(user.getName());
        userDataDto.setDisplayName(user.getDisplayName());
        userDataDto.setAvatarUrl(getUserAvatarSrc(user));
        userDataDto.setTimezone(timeZoneManager.getTimeZoneforUser(user).getID());
        if (isAdministrator(user)) {
            userDataDto.setNextFeedbackShow(userData.getNextFeedbackShow());
            userDataDto.setPluginRated(userData.getNextFeedbackShow() == -1);
            userDataDto.setFeedbackShowCount(userData.getFeedbackShowCount());
        }
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
                if (userDataDto.getCalendars() != null && !userDataDto.getCalendars().isEmpty())
                    for (Integer calendarId : userDataDto.getCalendars())
                        try {
                            Calendar calendar = calendarService.getCalendar(calendarId);
                            userCalendarService.addCalendarToUser(user.getKey(), calendar, true);
                        } catch (GetException e) {
                            log.warn("User attempt to add not existing calendar. User={}, calendar={}", user.getKey(), calendarId);
                        }
                return getUserDataDto(user, getUserData(user));
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

    public void updateUserLikeData(final ApplicationUser user, final boolean rated) {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                UserData userData = getUserData(user);
                userData.setFeedbackShowCount(userData.getFeedbackShowCount() + 1);
                if (rated)
                    userData.setNextFeedbackShow(-1);
                else {
                    DateTime nextShow = new DateTime(userData.getNextFeedbackShow());
                    if (nextShow.isBeforeNow())
                        nextShow = new DateTime();
                    int count = userData.getFeedbackShowCount();
                    if (count == 1 || count == 2)
                        nextShow = nextShow.plusWeeks(2);
                    else if (count == 3)
                        nextShow = nextShow.plusMonths(1);
                    else if (count == 4)
                        nextShow = nextShow.plusMonths(2);
                    else if (count == 5)
                        nextShow = nextShow.plusMonths(4);
                    else if (count >= 6)
                        nextShow = nextShow.plusMonths(8);
                    userData.setNextFeedbackShow(nextShow.getMillis());
                }
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

    private String getUserAvatarSrc(ApplicationUser user) {
        return avatarService.getAvatarURL(user, user, Avatar.Size.SMALL).toString();
    }
}
