package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Setter;
import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.UserCalendar;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@Setter
@SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
@XmlRootElement
public class CalendarDto {
    @XmlElement
    private int id;
    @XmlElement
    private String name;
    @XmlElement
    private String color;
    @XmlElement
    private String source;
    @XmlElement
    private boolean eventsEditable;
    @XmlElement
    private boolean changable;
    @XmlElement
    private boolean viewable;
    @XmlElement
    private boolean visible;
    @XmlElement
    private boolean favorite;
    @XmlElement
    private boolean hasError;
    @XmlElement
    private String error;
    @XmlElement
    private Integer usersCount;
    @XmlElement
    private List<QuickFilterDto> favouriteQuickFilters;
    @XmlElement
    private List<QuickFilterDto> selectedQuickFilters;

    public CalendarDto() {
    }

    public CalendarDto(UserCalendar userCalendar, Calendar calendar) {
        if (calendar == null && userCalendar != null) {
            this.id = userCalendar.getCalendarId();
            this.name = userCalendar.getName();
            this.color = userCalendar.getColor();
        } else if (calendar != null) {
            this.id = calendar.getID();
            this.name = calendar.getName();
            this.color = calendar.getColor();
            this.source = calendar.getSource();
        }
    }

    public Integer getUsersCount() {
        return usersCount;
    }
}
