package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.ProjectService;
import com.atlassian.jira.permission.GlobalPermissionKey;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.ActiveObjectsException;
import net.java.ao.Query;
import org.apache.commons.lang3.StringUtils;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.UserData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UserDataService {
    private final ActiveObjects ao;
    private final GroupManager groupManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final ProjectManager projectManager;
    private final ProjectService projectService;
    private final ProjectRoleManager projectRoleManager;

    public UserDataService(ActiveObjects ao,
                           GroupManager groupManager,
                           GlobalPermissionManager globalPermissionManager,
                           ProjectManager projectManager,
                           ProjectService projectService,
                           ProjectRoleManager projectRoleManager) {
        this.ao = ao;
        this.groupManager = groupManager;
        this.globalPermissionManager = globalPermissionManager;
        this.projectManager = projectManager;
        this.projectService = projectService;
        this.projectRoleManager = projectRoleManager;
    }

    @Nullable
    public UserData getUserData(final ApplicationUser user) {
        return getUserData(user.getKey());
    }

    public Set<Integer> getShowedCalendars(final UserData userData) {
        if (userData != null) {
            String showedCalendars = userData.getShowedCalendars();
            if (StringUtils.isNotEmpty(showedCalendars)) {
                String[] splittedShowedCalendars = showedCalendars.split(";");
                Set<Integer> result = new HashSet<Integer>(splittedShowedCalendars.length);
                for (String calendarIdStr : splittedShowedCalendars)
                    result.add(Integer.parseInt(calendarIdStr));
                return result;
            }
        }
        return new HashSet<Integer>(0);
    }

    public Set<Integer> getShowedCalendars(final ApplicationUser user) {
        return getShowedCalendars(getUserData(user));
    }

    public Set<Integer> getFavoriteCalendars(UserData userData) {
        if (userData != null) {
            String favoriteCalendars = userData.getFavoriteCalendars();
            String[] splittedFavoriteCalendars = StringUtils.split(favoriteCalendars, ";");
            if (splittedFavoriteCalendars != null) {
                Set<Integer> result = new HashSet<Integer>(splittedFavoriteCalendars.length);
                for (String calendarIdStr : splittedFavoriteCalendars)
                    result.add(Integer.parseInt(calendarIdStr));
                return result;
            }
        }
        return new HashSet<Integer>(0);
    }

    public Set<Integer> getFavoriteCalendars(ApplicationUser user) {
        return getFavoriteCalendars(getUserData(user));
    }

    public int getUsersCount(final Calendar calendar) {
        return ao.executeInTransaction(new TransactionCallback<Integer>() {
            @Override
            public Integer doInTransaction() {
                int calenderId = calendar.getID();
                UserData[] result = ao.find(UserData.class, Query.select().where("FAVORITE_CALENDARS = ? " +
                                                                                         "OR FAVORITE_CALENDARS LIKE ? " +
                                                                                         "OR FAVORITE_CALENDARS LIKE ? " +
                                                                                         "OR FAVORITE_CALENDARS LIKE ?",
                                                                                 calenderId,
                                                                                 calenderId + ";%",
                                                                                 "%;" + calenderId + ";%",
                                                                                 "%;" + calenderId));
                return result.length + 1;
            }
        });
    }

    @Nullable
    public UserData getUserData(final String userKey) {
        return ao.executeInTransaction(new TransactionCallback<UserData>() {
            @Override
            public UserData doInTransaction() {
                UserData[] userDatas = ao.find(UserData.class, Query.select().where("USER_KEY = ?", userKey));
                if (userDatas.length == 0)
                    return null;

                UserData userData = userDatas[0];
                if (userData.getIcalUid() == null) {
                    userData.setICalUid(UUID.randomUUID().toString().substring(0, 8));
                    userData.save();
                }
                return userData;
            }
        });
    }

    public UserData[] getUserData() {
        return ao.executeInTransaction(new TransactionCallback<UserData[]>() {
            @Override
            public UserData[] doInTransaction() {
                return ao.find(UserData.class);
            }
        });
    }

    public void updateUserData(final ApplicationUser user, final String view, final Boolean hideWeekedns) {
        updateUserData(user.getKey(), view, hideWeekedns);
    }

    public void updateUserData(final String userKey, final String view, final Boolean hideWeekends) {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                UserData userData = notTransactionalUpdateUserData(userKey, view, hideWeekends);
                userData.save();
                return null;
            }
        });
    }

    public void updateUserData(@Nonnull final String userKey, @Nonnull final String view, final boolean hideWeekends, @Nonnull final List<Integer> extraShowedCalendars) {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                UserData userData = notTransactionalUpdateUserData(userKey, view, hideWeekends);
                if (extraShowedCalendars.size() > 0) {
                    if (userData.getShowedCalendars() == null)
                        userData.setShowedCalendars(StringUtils.join(extraShowedCalendars, ";"));
                    else {
                        String[] split = StringUtils.split(userData.getShowedCalendars(), ';');
                        List<Integer> showedCalendarIds = new ArrayList<Integer>();

                        for (String showedCalendarIdStr : Arrays.asList(split))
                            showedCalendarIds.add(Integer.parseInt(showedCalendarIdStr));

                        for (Integer extraCalendarId : extraShowedCalendars) {
                            if (!showedCalendarIds.contains(extraCalendarId))
                                showedCalendarIds.add(extraCalendarId);
                        }
                        userData.setShowedCalendars(StringUtils.join(showedCalendarIds, ";"));
                    }
                }
                userData.save();
                return null;
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

    public UserData updateIcalUid(final ApplicationUser user) {
        return ao.executeInTransaction(new TransactionCallback<UserData>() {
            @Override
            public UserData doInTransaction() {
                UserData userData = getUserData(user);
                if (userData != null) {
                    userData.setICalUid(UUID.randomUUID().toString().substring(0, 8));
                    userData.save();
                }
                return userData;
            }
        });
    }

    public void updateFavorites(final ApplicationUser user, final List<Integer> calendarIds) {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                UserData userData = getUserData(user);
                if (userData == null)
                    throw new IllegalArgumentException(String.format("UserData for user %s not found.", user.getKey()));

                Set<Integer> showed = getShowedCalendars(userData);
                Set<Integer> favorite = getFavoriteCalendars(userData);
                showed.addAll(calendarIds);
                favorite.addAll(calendarIds);
                userData.setShowedCalendars(StringUtils.join(showed, ";"));
                userData.setFavoriteCalendars(StringUtils.join(favorite, ";"));
                userData.save();
                return null;
            }
        });
    }

    public void updateFavorites(final UserData userData, final Collection<Integer> calendarIds) {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                userData.setFavoriteCalendars(StringUtils.join(calendarIds, ";"));
                userData.save();
                return null;
            }
        });
    }

    public void removeFavorite(final ApplicationUser user, final Integer calendarId) {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                UserData userData = getUserData(user);
                if (userData == null)
                    throw new IllegalArgumentException(String.format("UserData for user %s not found.", user.getKey()));

                Set<Integer> showed = getShowedCalendars(userData);
                Set<Integer> favorites = getFavoriteCalendars(userData);
                showed.remove(calendarId);
                favorites.remove(calendarId);
                userData.setFavoriteCalendars(StringUtils.join(favorites, ";"));
                userData.setShowedCalendars(StringUtils.join(showed, ";"));
                userData.save();
                return null;
            }
        });
    }

    private UserData notTransactionalUpdateUserData(String userKey, String view, Boolean hideWeekedns) {
        UserData userData = getUserData(userKey);
        if (userData == null) {
            userData = ao.create(UserData.class);
            userData.setUserKey(userKey);
            userData.setHideWeekends(false);
            userData.setShowTime(false);
            userData.setICalUid(UUID.randomUUID().toString().substring(0, 8));
        }
        if (view != null)
            userData.setDefaultView(view);
        if (hideWeekedns != null)
            userData.setHideWeekends(hideWeekedns);
        return userData;
    }

    public boolean isCalendarShowedForCurrentUser(final ApplicationUser user, Calendar calendar) {
        UserData userData = findUserData(user);
        if (userData == null || StringUtils.isBlank(userData.getShowedCalendars()))
            return false;
        return Arrays.asList(userData.getShowedCalendars().split(";")).contains(String.valueOf(calendar.getID()));
    }

    public UserData findUserData(final ApplicationUser user) {
        UserData[] userDatas = ao.find(UserData.class, Query.select().where("USER_KEY = ?", user.getKey()));
        if (userDatas.length > 0)
            return userDatas[0];
        return null;
    }

    public List<String> getUserGroups(ApplicationUser user) {
        Collection<Group> groups = isAdministrator(user)
                ? groupManager.getAllGroups()
                : groupManager.getGroupsForUser(user.getName());
        List<String> result = new ArrayList<String>(groups.size());
        for (Group group : groups)
            result.add(group.getName());
        return result;
    }

    public Map<Long, String> getUserProjects(ApplicationUser user) {
        Map<Long, String> result = new LinkedHashMap<Long, String>();
        List<Project> allProjects = isAdministrator(user)
                ? projectManager.getProjectObjects()
                : projectService.getAllProjects(user).get();

        for (Project project : allProjects)
            result.put(project.getId(), project.getName());

        return result;
    }

    public Map<Long, String> getUserRoles(ApplicationUser user, long projectId) {
        Map<Long, String> result = new LinkedHashMap<Long, String>();
        Collection<ProjectRole> projectRoles;
        if (isAdministrator(user)) {
            projectRoles = projectRoleManager.getProjectRoles();
        } else {
            Project project = projectManager.getProjectObj(projectId);
            projectRoles = projectRoleManager.getProjectRoles(user, project);
        }

        for (ProjectRole role : projectRoles)
            result.put(role.getId(), role.getName());

        return result;
    }

    private boolean isAdministrator(ApplicationUser user) {
        return globalPermissionManager.hasPermission(GlobalPermissionKey.ADMINISTER, user);
    }
}
