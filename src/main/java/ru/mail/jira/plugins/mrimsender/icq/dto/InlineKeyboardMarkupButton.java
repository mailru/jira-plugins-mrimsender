package ru.mail.jira.plugins.mrimsender.icq.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InlineKeyboardMarkupButton {
    private String text;
    private String url;
    private String callbackData;
}
