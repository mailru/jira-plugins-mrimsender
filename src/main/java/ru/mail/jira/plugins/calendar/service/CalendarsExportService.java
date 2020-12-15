package ru.mail.jira.plugins.calendar.service;

import ru.mail.jira.plugins.calendar.model.ExportedCalendarsData;

import java.util.List;

public interface CalendarsExportService {
    void exportCalendars(String icalUid, List<String> exportedCalendarIds);

    ExportedCalendarsData findExportedCalendarsByIcalUid(String icalUid);
}
