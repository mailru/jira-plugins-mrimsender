package ru.mail.jira.plugins.myteam.myteam;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import ru.mail.jira.plugins.myteam.configuration.PluginData;
import ru.mail.jira.plugins.myteam.myteam.dto.FetchResponseDto;
import ru.mail.jira.plugins.myteam.myteam.dto.MessageResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.events.CallbackQueryEvent;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class MyteamApiClientImplTest {

    private PluginData pluginData;
    private MyteamApiClient myteamApiClient;
    @Before
    public void setUp() throws Exception {
        Unirest.setTimeouts(10_000, 300_000);
        Unirest.setObjectMapper(new ObjectMapper() {
            private org.codehaus.jackson.map.ObjectMapper jacksonObjectMapper = new org.codehaus.jackson.map.ObjectMapper();

            @Override
            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        this.pluginData = Mockito.mock(PluginData.class);
        when(pluginData.getToken()).thenReturn("001.0533637662.3485284314:1000000272");
        when(pluginData.getBotApiUrl()).thenReturn("https://api.internal.myteam.mail.ru/bot/v1");
        this.myteamApiClient = new MyteamApiClientImpl(this.pluginData);
    }

    @Ignore
    public void sendMessageText() throws IOException, UnirestException {
        HttpResponse<MessageResponse> httpResponse = myteamApiClient.sendMessageText("d.udovichenko@corp.mail.ru", "test text", null);
        System.out.println(httpResponse.getBody());
        assertTrue(httpResponse.getBody().isOk());
    }

    @Ignore
    public void getEvents() throws UnirestException {
        HttpResponse<FetchResponseDto> httpResponse = myteamApiClient.getEvents(0, 5);
        assertEquals(200, httpResponse.getStatus());
    }

    @Ignore
    public void answerCallbackQuery() throws UnirestException {
        HttpResponse<FetchResponseDto> httpResponse = myteamApiClient.getEvents(0, 5);
        List<CallbackQueryEvent> callbackQueryEventList = httpResponse.getBody()
                                                                      .getEvents()
                                                                      .stream()
                                                                      .filter(event -> event instanceof CallbackQueryEvent)
                                                                      .map(event -> (CallbackQueryEvent)event)
                                                                      .collect(Collectors.toList());
        if (callbackQueryEventList.size() > 0) {
            CallbackQueryEvent callbackQueryEvent = callbackQueryEventList.get(0);
            HttpResponse<JsonNode> jsonNodeHttpResponse = myteamApiClient.answerCallbackQuery(callbackQueryEvent.getQueryId(), "text here", true, null);
            assertEquals(200, jsonNodeHttpResponse.getStatus());
        }
    }
}