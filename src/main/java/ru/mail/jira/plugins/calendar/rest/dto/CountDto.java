package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement @Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CountDto {
    @XmlElement
    private long count;

    public CountDto(long count) {
        this.count = count;
    }
}
