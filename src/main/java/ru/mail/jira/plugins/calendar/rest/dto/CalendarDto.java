package ru.mail.jira.plugins.calendar.rest.dto;

import ru.mail.jira.plugins.calendar.model.Calendar;
import ru.mail.jira.plugins.calendar.model.UserCalendar;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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

    public void setSource(String source) {
        this.source = source;
    }

    public void setChangable(boolean changable) {
        this.changable = changable;
    }

    public void setViewable(boolean viewable) {
        this.viewable = viewable;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void setHasError(boolean withErrors) {
        this.hasError = withErrors;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setUsersCount(Integer usersCount) {
        this.usersCount = usersCount;
    }

    public Integer getUsersCount() {
        return usersCount;
    }
}
