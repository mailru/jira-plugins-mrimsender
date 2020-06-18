package ru.mail.jira.plugins.myteam.myteam.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;


@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class InlineKeyboardMarkupButton {
    private String text;
    private String url;
    private String callbackData;

    public static InlineKeyboardMarkupButton buildButtonWithoutUrl(String text, String callbackData) {
        InlineKeyboardMarkupButton inlineKeyboardMarkupButton = new InlineKeyboardMarkupButton();
        inlineKeyboardMarkupButton.setCallbackData(callbackData);
        inlineKeyboardMarkupButton.setText(text);
        return inlineKeyboardMarkupButton;
    }
}


