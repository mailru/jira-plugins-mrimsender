package ru.mail.jira.plugins.calendar.util;

import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.calendar.configuration.WorkingTimeDto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

public final class DateUtil {
    private DateUtil() {};

    public static Date addWorkTimeSeconds(
        boolean allDay, Date sourceDate, long seconds, long secondsPerWeek, long secondsPerDay,
        Set<Integer> workingDays, Set<java.time.LocalDate> nonWorkingDays, WorkingTimeDto workingTime, ZoneId zoneId
    ) {
        boolean reduceIfWorkDay = false;

        long weeks = seconds / secondsPerWeek;
        seconds = seconds % secondsPerWeek;

        long days = seconds / secondsPerDay;
        seconds = seconds % secondsPerDay;

        ZonedDateTime date = sourceDate
            .toInstant()
            .atZone(zoneId)
            .plusWeeks(weeks);

        LocalTime startTime = workingTime.getStartTime();
        LocalTime endTime = workingTime.getEndTime();

        java.time.LocalDate localDate = date.toLocalDate();
        if (allDay) {
            if (seconds > 0) {
                days++;
            }
        } else {
            LocalTime time = date.toLocalTime();

            if (seconds <= 0) {
                if (days > 0) {
                    reduceIfWorkDay = true;
                    seconds = secondsPerDay;
                }
            }
            if (seconds > 0) {
                if (time.isBefore(startTime)) {
                    date = ZonedDateTime.of(
                        localDate,
                        startTime,
                        zoneId
                    );
                    time = startTime;
                }

                if (time.isBefore(endTime)) {
                    long secondOfDay = time.toSecondOfDay() + seconds;
                    int endSecond = endTime.toSecondOfDay();
                    seconds = secondOfDay - endSecond;
                    //if (time + seconds) > end_time: seconds=(time+seconds)-end_time, time=end_time
                    if (seconds > 0) {
                        time = endTime;
                    } else {
                        time = LocalTime.ofSecondOfDay(secondOfDay);
                    }
                    date = ZonedDateTime.of(localDate, time, zoneId);
                }

                //if after end of day: date=date+1d, time=start_time+seconds
                if (seconds > 0) {
                    days++;
                    date = ZonedDateTime.of(
                        localDate,
                        startTime.plusSeconds(seconds),
                        zoneId
                    );
                }
            }
        }

        while (days > 0) {
            if (!allDay) {
                if (reduceIfWorkDay) {
                    boolean isWeekDay = workingDays.contains(date.getDayOfWeek().getValue());
                    boolean isNonWorkingDay = nonWorkingDays.contains(date.toLocalDate());
                    boolean isWorkDay = isWeekDay && !isNonWorkingDay;

                    if (isWorkDay) {
                        days--;
                    }
                    reduceIfWorkDay = false;

                    if (days == 0) {
                        //date = date.plusDays(1);
                        continue;
                    }
                }

                date = date.plusDays(1);
            }

            boolean isWeekDay = workingDays.contains(date.getDayOfWeek().getValue());
            boolean isNonWorkingDay = nonWorkingDays.contains(date.toLocalDate());
            boolean isWorkDay = isWeekDay && !isNonWorkingDay;

            if (isWorkDay) {
                days--;
            }

            if (allDay) {
                date = date.plusDays(1);
            }
        }

        return Date.from(date.toInstant());
    }

    //todo: more precise calculation with time
    public static int countWorkDays(LocalDate start, LocalDate end, List<Integer> workingDays, Set<java.time.LocalDate> nonWorkingDays) {
        if (start.isAfter(end) || start.isEqual(end)) {
            throw new RuntimeException("end is after today");
        }

        int i = 0;
        while (start.compareTo(end) < 0) {
            if (workingDays.contains(start.getDayOfWeek().getValue()) && !nonWorkingDays.contains(start)) {
                i++;
            }
            start = start.plusDays(1);
        }

        return i;
    }
}
