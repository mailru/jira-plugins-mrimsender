package ru.mail.jira.plugins.calendar;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Migrator {
    private final static Logger log = LoggerFactory.getLogger(Migrator.class);

    private final CalendarMigrator calendarMigrator;
    private final UserPreferenceMigrator userPreferenceMigrator;

    public Migrator(CalendarMigrator calendarMigrator, UserPreferenceMigrator userPreferenceMigrator) {
        this.calendarMigrator = calendarMigrator;
        this.userPreferenceMigrator = userPreferenceMigrator;
    }

    public void migrate() throws Exception {
        log.info("Migration has been started");
        Map<Long, Integer> oldToNewCalendarIds = calendarMigrator.migrateCalendars();
        userPreferenceMigrator.migrate(oldToNewCalendarIds);
    }
}
