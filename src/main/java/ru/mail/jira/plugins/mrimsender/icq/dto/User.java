package ru.mail.jira.plugins.mrimsender.icq.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String firstName;
    private String lastName;
    private String nick;
    private String userId;
}
