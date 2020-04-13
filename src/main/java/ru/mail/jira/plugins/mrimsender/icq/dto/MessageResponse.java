package ru.mail.jira.plugins.mrimsender.icq.dto;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MessageResponse {
    private long msgId;
    private boolean ok;
    private String description;
}
