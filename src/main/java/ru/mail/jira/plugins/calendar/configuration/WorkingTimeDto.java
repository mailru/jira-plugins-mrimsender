package ru.mail.jira.plugins.calendar.configuration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import ru.mail.jira.plugins.calendar.util.LocalTimeSerializer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.time.LocalTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@XmlRootElement
public class WorkingTimeDto {
    @XmlElement
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime startTime;
    @XmlElement
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime endTime;
}
