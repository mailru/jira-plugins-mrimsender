package ru.mail.jira.plugins.mrimsender.icq.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.map.annotate.JsonSerialize;


@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@Getter
@Setter
@ToString
public class InlineKeyboardMarkupButton {
    private String text;
    private String url;
    private String callbackData;
}


