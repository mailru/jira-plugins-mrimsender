package ru.mail.jira.plugins.mrimsender.icq.dto;

import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;

import java.util.List;

public class FetchResponseDto {
    private List<Event<?>> events;

    private Boolean ok;

    public List<Event<?>> getEvents() {
        return events;
    }

    public Boolean isOk() {
        return ok;
    }

    public void setEvents(List<Event<?>> events) {
        this.events = events;
    }

    public void setOk(Boolean ok) {
        this.ok = ok;
    }

    @Override
    public String toString() {
        return "FetchResponseDto{" +
                "events=" + events +
                ", ok=" + ok +
                '}';
    }
}
