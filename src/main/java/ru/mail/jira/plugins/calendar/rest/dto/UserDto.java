package ru.mail.jira.plugins.calendar.rest.dto;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@XmlRootElement
@Getter @Setter
public class UserDto {
    @XmlElement
    private String key;
    @XmlElement
    private String name;
    @XmlElement
    private String displayName;
    @XmlElement
    private String avatarUrl;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDto userDto = (UserDto) o;
        return Objects.equals(key, userDto.key);
    }

    @Override
    public int hashCode() {

        return Objects.hash(key);
    }
}