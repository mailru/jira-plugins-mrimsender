package ru.mail.jira.plugins.calendar.upgrades.v3;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.Permission;
import ru.mail.jira.plugins.calendar.model.SubjectType;
import ru.mail.jira.plugins.calendar.model.UserCalendar;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.calendar.model.archive.Share;
import ru.mail.jira.plugins.calendar.service.PermissionUtils;

import java.util.HashSet;
import java.util.Set;

public class Version3UpgradeTask implements ActiveObjectsUpgradeTask {
    private final static Logger log = LoggerFactory.getLogger(Version3UpgradeTask.class);

    @Override
    public ModelVersion getModelVersion() {
        return ModelVersion.valueOf("3");
    }

    @Override
    public void upgrade(ModelVersion currentVersion, final ActiveObjects ao) {
        log.info("Current version " + currentVersion.toString());
        if (currentVersion.isOlderThan(getModelVersion())) {
            ao.migrate(Share.class, Calendar.class, Permission.class, UserCalendar.class, UserData.class);
            log.info("Run upgrade task to version 3.");
            ao.executeInTransaction(new TransactionCallback<Void>() {
                @Override
                public Void doInTransaction() {
                    Calendar[] calendars = ao.find(Calendar.class);
                    for (UserData userData : ao.find(UserData.class)) {
                        Set<Integer> favoriteSet = getFavoriteCalendars(userData);
                        Set<Integer> showedSet = getShowedCalendars(userData);
                        favoriteSet.addAll(showedSet);
                        for (Calendar calendar : calendars)
                            if (calendar.getAuthorKey().equals(userData.getUserKey()))
                                favoriteSet.add(calendar.getID());

                        for (Integer calendarId : favoriteSet) {
                            Calendar calendar = ao.get(Calendar.class, calendarId);
                            if (calendar != null) {
                                UserCalendar userCalendar = ao.create(UserCalendar.class);
                                userCalendar.setName(calendar.getName());
                                userCalendar.setColor(calendar.getColor());
                                userCalendar.setEnabled(showedSet.contains(calendarId));
                                userCalendar.setCalendarId(calendar.getID());
                                userCalendar.setUserKey(userData.getUserKey());
                                userCalendar.save();
                            }
                        }
                    }

                    for (Share share : ao.find(Share.class)) {
                        Permission permission = ao.create(Permission.class);
                        permission.setUse(true);
                        permission.setCalendar(share.getCalendar());
                        if (StringUtils.isNotEmpty(share.getGroup())) {
                            permission.setSubjectType(SubjectType.GROUP);
                            permission.setSubject(share.getGroup());
                        } else {
                            permission.setSubjectType(SubjectType.PROJECT_ROLE);
                            permission.setSubject(PermissionUtils.projectRoleSubject(share.getProject(), share.getRole()));
                        }
                        permission.save();
                    }
                    for (Calendar calendar : calendars) {
                        Permission permission = ao.create(Permission.class);
                        permission.setUse(true);
                        permission.setAdmin(true);
                        permission.setCalendar(calendar);
                        permission.setSubjectType(SubjectType.USER);
                        permission.setSubject(calendar.getAuthorKey());
                        permission.save();
                    }
                    return null;
                }
            });
        }
    }

    private Set<Integer> getShowedCalendars(final UserData userData) {
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

    private Set<Integer> getFavoriteCalendars(UserData userData) {
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
}
