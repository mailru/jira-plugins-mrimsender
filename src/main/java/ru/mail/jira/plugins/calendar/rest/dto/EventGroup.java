package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement @Getter @Setter @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventGroup {
    @XmlElement
    private String id;
    @XmlElement
    private String name;
    @XmlElement
    private String avatar;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventGroup that = (EventGroup) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }
}
