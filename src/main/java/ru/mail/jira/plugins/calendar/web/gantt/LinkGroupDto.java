package ru.mail.jira.plugins.calendar.web.gantt;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class LinkGroupDto {
    private String calendarName;
    private Integer calendarId;
    private List<LinkDto> links;
}
