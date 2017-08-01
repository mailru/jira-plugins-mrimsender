package ru.mail.jira.plugins.calendar.service.reminder;

import java.time.Instant;

public interface ReminderService {
    void triggerNotificationsInRange(long since, long until);
}
