package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
public class EventMilestoneDto {
    private String fieldKey;
    private String fieldName;
    private String date;
}
