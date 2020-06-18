package ru.mail.jira.plugins.myteam.myteam;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import ru.mail.jira.plugins.myteam.myteam.dto.FetchResponseDto;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.MessageResponse;

import java.io.IOException;
import java.util.List;

public interface MyteamApiClient {
    public HttpResponse<MessageResponse> sendMessageText(String chatId, String text, List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup) throws UnirestException, IOException;

    public HttpResponse<MessageResponse> sendMessageText(String chatId, String text) throws UnirestException, IOException;

    public HttpResponse<FetchResponseDto> getEvents(long lastEventId, long pollTime) throws UnirestException;

    public HttpResponse<JsonNode> answerCallbackQuery(String queryId, String text, boolean showAlert, String url) throws UnirestException;

    public HttpResponse<JsonNode> answerCallbackQuery(String queryId) throws UnirestException;

    public void updateSettings();

    public HttpResponse<MessageResponse> editMessageText(String chatId, long messageId, String text, List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup) throws  UnirestException, IOException;
}
