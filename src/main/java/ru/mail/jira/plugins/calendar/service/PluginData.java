package ru.mail.jira.plugins.calendar.service;

public interface PluginData {
    Long getReminderLastRun();

    void setReminderLastRun(long timestamp);
}
