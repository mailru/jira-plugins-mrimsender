package ru.mail.jira.plugins.myteam.myteam.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import ru.mail.jira.plugins.myteam.myteam.dto.events.IcqEvent;

import java.util.List;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class FetchResponseDto {
    private List<IcqEvent> events;
    private boolean ok;

    public List<IcqEvent> getEvents() {
        return events;
    }
}
