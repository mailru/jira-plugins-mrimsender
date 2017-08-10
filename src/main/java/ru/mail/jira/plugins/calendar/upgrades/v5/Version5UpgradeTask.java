package ru.mail.jira.plugins.calendar.upgrades.v5;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.activeobjects.external.ActiveObjectsUpgradeTask;
import com.atlassian.activeobjects.external.ModelVersion;
import net.java.ao.DBParam;
import ru.mail.jira.plugins.calendar.model.EventType;

public class Version5UpgradeTask implements ActiveObjectsUpgradeTask {
    @Override
    public ModelVersion getModelVersion() {
        return ModelVersion.valueOf("5");
    }

    @Override
    public void upgrade(ModelVersion modelVersion, ActiveObjects ao) {
        ao.migrate(EventType.class);

        ao.create(
            EventType.class,
            new DBParam("CALENDAR_ID", null),
            new DBParam("I18N_NAME", "ru.mail.jira.plugins.calendar.customEvents.type.event"),
            new DBParam("NAME", "Event"),
            new DBParam("SYSTEM", Boolean.TRUE),
            new DBParam("DELETED", Boolean.FALSE),
            new DBParam("AVATAR", "event")
        );

        ao.create(
            EventType.class,
            new DBParam("CALENDAR_ID", null),
            new DBParam("I18N_NAME", "ru.mail.jira.plugins.calendar.customEvents.type.leave"),
            new DBParam("NAME", "Leave"),
            new DBParam("SYSTEM", Boolean.TRUE),
            new DBParam("DELETED", Boolean.FALSE),
            new DBParam("AVATAR", "leave")
        );

        ao.create(
            EventType.class,
            new DBParam("CALENDAR_ID", null),
            new DBParam("I18N_NAME", "ru.mail.jira.plugins.calendar.customEvents.type.travel"),
            new DBParam("NAME", "Travel"),
            new DBParam("SYSTEM", Boolean.TRUE),
            new DBParam("DELETED", Boolean.FALSE),
            new DBParam("AVATAR", "travel")
        );

        ao.create(
            EventType.class,
            new DBParam("CALENDAR_ID", null),
            new DBParam("I18N_NAME", "ru.mail.jira.plugins.calendar.customEvents.type.birthday"),
            new DBParam("NAME", "Birthday"),
            new DBParam("SYSTEM", Boolean.TRUE),
            new DBParam("DELETED", Boolean.FALSE),
            new DBParam("AVATAR", "birthday")
        );
    }
}
