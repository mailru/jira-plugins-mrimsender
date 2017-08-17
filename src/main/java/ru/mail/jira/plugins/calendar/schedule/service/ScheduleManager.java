package ru.mail.jira.plugins.calendar.schedule.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.schedule.model.Schedule;

import java.util.Date;

@Component
public class ScheduleManager {
    private final ActiveObjects ao;

    @Autowired
    public ScheduleManager(@ComponentImport ActiveObjects ao) {
        this.ao = ao;
    }

    public Schedule getSchedule(final int id) {
        return ao.executeInTransaction(new TransactionCallback<Schedule>() {
            @Override
            public Schedule doInTransaction() {
                Schedule schedule = ao.get(Schedule.class, id);
                if (schedule == null)
                    throw new IllegalArgumentException(String.format("Schedule is not found by id %s", id));
                return schedule;
            }
        });
    }

    public Schedule[] getSchedules() {
        return ao.executeInTransaction(new TransactionCallback<Schedule[]>() {
            @Override
            public Schedule[] doInTransaction() {
                return ao.find(Schedule.class);
            }
        });
    }

    public Schedule[] getSchedules(final String creatorKey, final boolean deleted) {
        return ao.executeInTransaction(new TransactionCallback<Schedule[]>() {
            @Override
            public Schedule[] doInTransaction() {
                return ao.find(Schedule.class, Query.select().where("CREATOR_KEY = ? AND DELETED = ?", creatorKey, deleted));
            }
        });
    }

    public Schedule[] getSchedules(final boolean deleted) {
        return ao.executeInTransaction(new TransactionCallback<Schedule[]>() {
            @Override
            public Schedule[] doInTransaction() {
                return ao.find(Schedule.class, Query.select().where("DELETED = ?", deleted));
            }
        });
    }

    public Schedule createSchedule(final long sourceIssueId, final String name, final String creatorKey, final String mode, final String cronExpression) {
        return ao.executeInTransaction(new TransactionCallback<Schedule>() {
            @Override
            public Schedule doInTransaction() {
                Schedule schedule = ao.create(Schedule.class);
                schedule.setSourceIssueId(sourceIssueId);
                schedule.setName(name);
                schedule.setCreatorKey(creatorKey);
                schedule.setMode(mode);
                schedule.setCronExpression(cronExpression);
                schedule.setDeleted(false);
                schedule.save();
                return schedule;
            }
        });
    }

    public Schedule updateSchedule(final int id, final String name, final String mode, final String cronExpression) {
        return ao.executeInTransaction(new TransactionCallback<Schedule>() {
            @Override
            public Schedule doInTransaction() {
                Schedule schedule = getSchedule(id);
                schedule.setName(name);
                schedule.setMode(mode);
                schedule.setCronExpression(cronExpression);
                schedule.setDeleted(false);
                schedule.save();
                return schedule;
            }
        });
    }

    public Schedule updateSchedule(final int id, final int runCount, final Date lastRun, final long lastCreatedIssueId) {
        return ao.executeInTransaction(new TransactionCallback<Schedule>() {
            @Override
            public Schedule doInTransaction() {
                Schedule schedule = getSchedule(id);
                schedule.setRunCount(runCount);
                schedule.setLastRun(lastRun);
                schedule.setLastCreatedIssueId(lastCreatedIssueId);
                schedule.save();
                return schedule;
            }
        });
    }

    public void deleteSchedule(final int id) {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                Schedule schedule = getSchedule(id);
                schedule.setDeleted(true);
                schedule.save();
                return null;
            }
        });
    }

    public void deleteSchedules() {
        ao.executeInTransaction(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction() {
                ao.delete(getSchedules());
                return null;
            }
        });
    }
}
