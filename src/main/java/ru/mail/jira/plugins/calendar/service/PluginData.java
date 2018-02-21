package ru.mail.jira.plugins.calendar.service;

import ru.mail.jira.plugins.calendar.configuration.WorkingTimeDto;

import java.util.List;

public interface PluginData {
    Long getReminderLastRun();

    void setReminderLastRun(long timestamp);

    List<Integer> getWorkingDays();
    void setWorkingDays(String workingDays);

    WorkingTimeDto getWorkingTime();
    void setWorkingTime(WorkingTimeDto workingTime);
}
