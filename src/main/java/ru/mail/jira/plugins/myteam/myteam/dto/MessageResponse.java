package ru.mail.jira.plugins.myteam.myteam.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageResponse {
    private long msgId;
    private boolean ok;
    private String description;
}
