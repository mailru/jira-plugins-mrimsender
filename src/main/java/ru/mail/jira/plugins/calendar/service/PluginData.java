package ru.mail.jira.plugins.calendar.service;

import java.util.List;

public interface PluginData {
    Long getReminderLastRun();

    void setReminderLastRun(long timestamp);

    List<Integer> getWorkingDays();
    void setWorkingDays(String workingDays);
}
