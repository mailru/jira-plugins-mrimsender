package ru.mail.jira.plugins.calendar.migrator;


import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.UserData;
import ru.mail.jira.plugins.calendar.service.CalendarService;
import ru.mail.jira.plugins.calendar.service.UserDataService;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class Migrator implements LifecycleAware, InitializingBean, DisposableBean {
    private final static Logger log = LoggerFactory.getLogger(Migrator.class);
    private final static String PLUGIN_KEY_VERSION_1 = "SimpleCalendar";
    private static final String CALENDARS_HAVE_BEEN_MIGRATED_KEY = "chbm";
    private static final String PLUGIN_KEY = "ru.mail.jira.plugins.mailrucal";

    private final Set<LifecycleEvent> lifecycleEvents = EnumSet.noneOf(LifecycleEvent.class);

    private final ActiveObjects ao;
    private final EventPublisher eventPublisher;
    private final CalendarMigrator calendarMigrator;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final UserPreferenceMigrator userPreferenceMigrator;
    private final UserDataService userDataService;
    private final CalendarService calendarService;

    public Migrator(ActiveObjects ao, EventPublisher eventPublisher, CalendarMigrator calendarMigrator, PluginSettingsFactory pluginSettingsFactory, UserPreferenceMigrator userPreferenceMigrator, UserDataService userDataService, CalendarService calendarService) {
        this.ao = ao;
        this.eventPublisher = eventPublisher;
        this.calendarMigrator = calendarMigrator;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.userPreferenceMigrator = userPreferenceMigrator;
        this.userDataService = userDataService;
        this.calendarService = calendarService;
    }

    private void migrateVersion1to2() throws Exception {
        log.info("Migration from version 1.* to 2.* has been started.");
        Map<Long, Integer> oldToNewCalendarIds = calendarMigrator.migrateCalendars();
        userPreferenceMigrator.migrate(oldToNewCalendarIds);
    }

    private void checkOldCalendars() {
        try {
            PluginSettings pluginSettings = pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY_VERSION_1);
            if (pluginSettings.get(CALENDARS_HAVE_BEEN_MIGRATED_KEY) == null) {
                log.info("Calendar have not been migrated");
                migrateVersion1to2();
                pluginSettings.put(CALENDARS_HAVE_BEEN_MIGRATED_KEY, "migrated");
            } else {
                log.info("Calendar have been migrated earlier");
            }
        } catch (Exception e) {
            log.error("Error while trying to check old calendars", e);
        }
    }

    private void migrateSharedToFavorite() throws Exception {
        log.info("Migration from 'shared to favorite' has been started.");
        try {
            UserData[] userDataArray = userDataService.getUserData();
            for (UserData userData : userDataArray) {
                Set<Integer> showed = userDataService.getShowedCalendars(userData);
                Set<Integer> favorite = userDataService.getFavoriteCalendars(userData);
                if (!favorite.containsAll(showed)) {
                    // Remove isMy calendars from showed
                    for (Iterator<Integer> i = showed.iterator(); i.hasNext(); ) {
                        Calendar calendar = calendarService.getCalendar(i.next());
                        if (calendar != null && userData.getUserKey().equals(calendar.getAuthorKey()))
                            i.remove();
                    }
                    favorite.addAll(showed);
                    userDataService.updateFavorites(userData, favorite);
                }
            }
            log.info("Calendars have been migrated successfully");
        } catch (Exception e) {
            log.error("Error while migrating 'shared to favorite'", e);
        }
    }

    @Override
    public void onStart() {
        onLifecycleEvent(LifecycleEvent.LIFECYCLE_AWARE_ON_START);
    }

    @Override
    public void destroy() throws Exception {
        unregisterListener();
    }

    @EventListener
    public void onPluginEnabled(PluginEnabledEvent event) {
        if (PLUGIN_KEY.equals(event.getPlugin().getKey())) {
            onLifecycleEvent(LifecycleEvent.PLUGIN_ENABLED);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        registerListener();
        onLifecycleEvent(LifecycleEvent.AFTER_PROPERTIES_SET);
    }

    private void launch() throws Exception {
        log.info("Launch migration tasks.");
        initActiveObjects();
        checkOldCalendars();
        migrateSharedToFavorite();
        log.info("Migration ends successfully.");
    }

    private void onLifecycleEvent(LifecycleEvent event) {
        if (isLifecycleReady(event)) {
            unregisterListener();
            try {
                launch();
            } catch (Exception ex) {
                log.error("Unexpected error during launch", ex);
            }
        }
    }

    synchronized private boolean isLifecycleReady(LifecycleEvent event) {
        return lifecycleEvents.add(event) && lifecycleEvents.size() == LifecycleEvent.values().length;
    }

    private void registerListener() {
        eventPublisher.register(this);
    }

    private void unregisterListener() {
        eventPublisher.unregister(this);
    }

    private void initActiveObjects() {
        ao.flushAll();
    }

    enum LifecycleEvent {
        AFTER_PROPERTIES_SET,
        PLUGIN_ENABLED,
        LIFECYCLE_AWARE_ON_START
    }
}
