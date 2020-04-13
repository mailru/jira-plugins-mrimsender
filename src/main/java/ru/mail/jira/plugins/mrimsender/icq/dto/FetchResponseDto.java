package ru.mail.jira.plugins.mrimsender.icq.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;

import java.util.List;

@Getter
@Setter
@ToString
public class FetchResponseDto {
    private List<Event> events;
    private boolean ok;
}
