package ru.mail.jira.plugins.calendar.configuration;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalTime;

@Getter @Setter
@XmlRootElement
public class WorkingTimeDto {
    @XmlElement
    private LocalTime startTime;
    @XmlElement
    private LocalTime endTime;
}
