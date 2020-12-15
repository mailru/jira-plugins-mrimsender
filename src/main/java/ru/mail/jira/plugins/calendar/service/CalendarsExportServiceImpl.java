package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.mail.jira.plugins.calendar.model.ExportedCalendarsData;

import javax.annotation.Nullable;
import java.util.List;

@Service
public class CalendarsExportServiceImpl implements CalendarsExportService {
    private final ActiveObjects ao;

    @Autowired
    public CalendarsExportServiceImpl(@ComponentImport ActiveObjects ao) {
        this.ao = ao;
    }

    @Override
    public void exportCalendars(String icalUid, List<String> exportedCalendarIds) {
        if (icalUid == null || exportedCalendarIds == null)
            return;

        ExportedCalendarsData[] alreadyExported = ao.find(ExportedCalendarsData.class, Query.select().where("ICAL_UID = ?", icalUid));
        if (alreadyExported != null && alreadyExported.length > 0) {
            ao.deleteWithSQL(ExportedCalendarsData.class, "ICAL_UID = ?", icalUid);
        }
        String exportedCalendarsStr = String.join(",", exportedCalendarIds);
        ExportedCalendarsData exportedCalendarsData = ao.create(ExportedCalendarsData.class);
        exportedCalendarsData.setCalendarIds(exportedCalendarsStr);
        exportedCalendarsData.setICalUid(icalUid);
        exportedCalendarsData.save();
    }

    @Override
    @Nullable
    public ExportedCalendarsData findExportedCalendarsByIcalUid(String icalUid) {
        if (icalUid == null)
            return null;

        ExportedCalendarsData[] alreadyExported = ao.find(ExportedCalendarsData.class, Query.select().where("ICAL_UID = ?", icalUid));
        if (alreadyExported != null && alreadyExported.length > 0) {
            return alreadyExported[0];
        }
        return null;
    }
}
