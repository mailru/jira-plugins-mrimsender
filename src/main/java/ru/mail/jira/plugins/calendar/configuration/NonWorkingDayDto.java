package ru.mail.jira.plugins.calendar.configuration;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Getter
@Setter
@XmlRootElement
public class NonWorkingDayDto {
    @XmlElement
    private int id;
    @XmlElement
    private String date;
    @XmlElement
    private String description;

    public NonWorkingDayDto() {
    }

    public NonWorkingDayDto(int id, String date, String description) {
        this.id = id;
        this.date = date;
        this.description = description;
    }
}
