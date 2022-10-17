/* (C)2022 */
package ru.mail.jira.plugins.myteam.commons;

import com.atlassian.jira.util.JiraUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class CalendarUtils {
  private final DateTimeFormatter dateTimeFormatter =
      java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  @Nullable
  public String formatDate(@Nullable Date date) {
    if (date == null) return null;
    return date.toInstant()
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
        .format(dateTimeFormatter);
  }

  @Nullable
  public Date parseDate(@Nullable String date) {
    if (date == null) return null;
    return Date.from(
        LocalDateTime.parse(date, dateTimeFormatter).atZone(ZoneId.systemDefault()).toInstant());
  }

  @Nullable
  public Date convertToDate(LocalDateTime dateTime) {
    if (dateTime == null) return null;
    return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
  }

  public int get12HourTime(int hours) {
    return hours == 12 ? hours : hours % 12;
  }

  public int get24HourTime(int hours, String meridianIndicator) {
    return JiraUtils.get24HourTime(meridianIndicator, hours);
  }

  public String getMeridianIndicator(int hours) {
    return hours >= 12 ? "PM" : "AM";
  }
}
