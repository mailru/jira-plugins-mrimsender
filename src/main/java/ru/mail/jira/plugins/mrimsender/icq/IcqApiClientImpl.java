package ru.mail.jira.plugins.mrimsender.icq;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.icq.dto.FetchResponseDto;
import ru.mail.jira.plugins.mrimsender.icq.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.mrimsender.icq.dto.MessageResponse;

import java.util.List;

public class IcqApiClientImpl implements IcqApiClient {
    private final PluginData pluginData;
    public final String BASE_API_URL = "https://api.icq.net/bot/v1";

    public IcqApiClientImpl(PluginData pluginData) {
        this.pluginData = pluginData;
    }

    public HttpResponse<MessageResponse> sendMessageText(String chatId, String text, List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup) throws UnirestException {
        if (inlineKeyboardMarkup == null)
            return Unirest.get(BASE_API_URL)
                          .queryString("token", pluginData.getToken())
                          .queryString("chatId", chatId)
                          .queryString("text", text)
                          .asObject(MessageResponse.class);
        return Unirest.get(BASE_API_URL)
                      .queryString("token", pluginData.getToken())
                      .queryString("chatId", chatId)
                      .queryString("text", text)
                      .queryString("inlineKeyboardMarkup", inlineKeyboardMarkup)
                      .asObject(MessageResponse.class);
    }


    public HttpResponse<FetchResponseDto> getEvents(long lastEventId, long pollTime) throws UnirestException {
        return Unirest.get(BASE_API_URL)
                      .queryString("token", pluginData.getToken())
                      .queryString("lastEventId", lastEventId)
                      .queryString("pollTime", pollTime)
                      .asObject(FetchResponseDto.class);
    }

    public HttpResponse<JsonNode> getAnswerCallbackQuery(String queryId, String text, boolean showAlert, String url) throws UnirestException {
        return Unirest.get(BASE_API_URL)
                      .queryString("queryId", queryId)
                      .queryString("text", text)
                      .queryString("showAlert", showAlert)
                      .queryString("url", url)
                      .asJson();
    }

}
