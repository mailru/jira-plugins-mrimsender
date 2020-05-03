package ru.mail.jira.plugins.mrimsender.icq;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import ru.mail.jira.plugins.mrimsender.icq.dto.FetchResponseDto;
import ru.mail.jira.plugins.mrimsender.icq.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.mrimsender.icq.dto.MessageResponse;

import java.io.IOException;
import java.util.List;

public interface IcqApiClient {
    public HttpResponse<MessageResponse> sendMessageText(String chatId, String text, List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup) throws UnirestException, IOException;

    public HttpResponse<MessageResponse> sendMessageText(String chatId, String text) throws UnirestException, IOException;

    public HttpResponse<FetchResponseDto> getEvents(long lastEventId, long pollTime) throws UnirestException;

    public HttpResponse<JsonNode> answerCallbackQuery(String queryId, String text, boolean showAlert, String url) throws UnirestException;

    public HttpResponse<JsonNode> answerCallbackQuery(String queryId) throws UnirestException;

    public void updateToken();

    public HttpResponse<MessageResponse> editMessageText(String chatId, long messageId, String text, List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup) throws  UnirestException, IOException;
}
