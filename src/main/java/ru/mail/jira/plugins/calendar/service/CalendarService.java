package ru.mail.jira.plugins.calendar.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.exception.GetException;
import com.atlassian.jira.user.ApplicationUser;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.rest.dto.CalendarDto;
import ru.mail.jira.plugins.calendar.rest.dto.CalendarSettingDto;

@Transactional
public interface CalendarService {
    Calendar getCalendar(int id) throws GetException;

    CalendarSettingDto getCalendarSettingDto(ApplicationUser user, int id) throws GetException;

    CalendarDto updateCalendar(ApplicationUser user, CalendarSettingDto calendarSettingDto) throws GetException;

    void updateCalendarVisibility(int calendarId, ApplicationUser user, boolean visible);

    void deleteCalendar(ApplicationUser user, int calendarId) throws GetException;

    CalendarDto[] getAllCalendars(ApplicationUser user);

    CalendarDto[] getUserCalendars(ApplicationUser user);

    CalendarDto[] findCalendars(ApplicationUser user, Integer[] calendarIds);

    CalendarDto createCalendar(ApplicationUser user, CalendarSettingDto calendarSettingDto) throws GetException;
}
