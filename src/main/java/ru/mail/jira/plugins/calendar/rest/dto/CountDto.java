package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement @Getter @Setter
public class CountDto {
    @XmlElement
    private long count;

    public CountDto(long count) {
        this.count = count;
    }
}
