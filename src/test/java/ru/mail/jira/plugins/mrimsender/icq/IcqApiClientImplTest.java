package ru.mail.jira.plugins.mrimsender.icq;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.icq.dto.FetchResponseDto;
import ru.mail.jira.plugins.mrimsender.icq.dto.MessageResponse;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.CallbackQueryEvent;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class IcqApiClientImplTest {

    private PluginData pluginData;
    private IcqApiClient icqApiClient;
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
        when(pluginData.getToken()).thenReturn("001.0352397737.0323867025:751619011");
        this.icqApiClient = new IcqApiClientImpl(this.pluginData);
    }

    @Test
    public void sendMessageText() throws IOException, UnirestException {
        HttpResponse<MessageResponse> httpResponse = icqApiClient.sendMessageText("d.udovichenko@corp.mail.ru", "test text", null);
        System.out.println(httpResponse.getBody());
        assertTrue(httpResponse.getBody().isOk());
    }

    @Test
    public void getEvents() throws UnirestException {
        HttpResponse<FetchResponseDto> httpResponse = icqApiClient.getEvents(0, 5);
        assertEquals(200, httpResponse.getStatus());
    }

    @Test
    public void answerCallbackQuery() throws UnirestException {
        HttpResponse<FetchResponseDto> httpResponse = icqApiClient.getEvents(0, 5);
        List<CallbackQueryEvent> callbackQueryEventList = httpResponse.getBody()
                                                                      .getEvents()
                                                                      .stream()
                                                                      .filter(event -> event instanceof CallbackQueryEvent)
                                                                      .map(event -> (CallbackQueryEvent)event)
                                                                      .collect(Collectors.toList());
        if (callbackQueryEventList.size() > 0) {
            CallbackQueryEvent callbackQueryEvent = callbackQueryEventList.get(0);
            HttpResponse<JsonNode> jsonNodeHttpResponse = icqApiClient.answerCallbackQuery(callbackQueryEvent.getQueryId(), "text here", true, null);
            assertEquals(200, jsonNodeHttpResponse.getStatus());
        }
    }
}