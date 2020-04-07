package ru.mail.jira.plugins.mrimsender;

import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import ru.mail.jira.plugins.mrimsender.configuration.PluginData;
import ru.mail.jira.plugins.mrimsender.icq.dto.FetchResponseDto;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.Event;
import ru.mail.jira.plugins.mrimsender.icq.dto.events.NewMessageEvent;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class IcqEventsFetcherTest {
    private PluginData pluginData;

    @Before
    public void init() {
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
    }

    @Ignore
    public void fetchIcqEvents() throws UnirestException, IOException {
        /*HttpResponse<JsonNode> jsonHttpResponse = Unirest.get(ApiUrlsHelper.getEventsUrl(pluginData.getToken(), 3, 60)).asJson();
        System.out.println(jsonHttpResponse.getBody());
        HttpResponse<FetchResponseDto> eventHttpResponse = Unirest.get(ApiUrlsHelper.getEventsUrl(pluginData.getToken(), 3, 60)).asObject(FetchResponseDto.class);

        System.out.println(eventHttpResponse.getBody());
        System.out.println(eventHttpResponse.getBody().getEvents());*/
    }

    @Test
    public void deserializationFetchResponseTest() throws IOException {
        String example = "{\"ok\":true,\"events\":[{\"eventId\":1,\"payload\":{\"chat\":{\"chatId\":\"example@example.ru\",\"type\":\"private\"},\"msgId\":\"6811058128403038841\",\"from\":{\"firstName\":\"Данил\",\"userId\":\"example@example.ru\"},\"text\":\"meh\",\"timestamp\":1585823048},\"type\":\"newMessage\"}]}\n";
        org.codehaus.jackson.map.ObjectMapper objectMapper = new org.codehaus.jackson.map.ObjectMapper();
        FetchResponseDto fetchResponseDto = objectMapper.readValue(example, FetchResponseDto.class);
        assertTrue(fetchResponseDto.isOk());
        assertEquals(fetchResponseDto.getEvents().size(), 1);
    }

    @Test
    public void deserializationNewMessageEventTest() throws IOException {
        String example = "{\"eventId\":1,\"payload\":{\"chat\":{\"chatId\":\"example@example.ru\",\"type\":\"private\"},\"msgId\":\"6811058128403038841\",\"from\":{\"firstName\":\"Данил\",\"userId\":\"example@example.ru\"},\"text\":\"meh\",\"timestamp\":1585823048},\"type\":\"newMessage\"}";
        org.codehaus.jackson.map.ObjectMapper objectMapper = new org.codehaus.jackson.map.ObjectMapper();
        Event<?> e = objectMapper.readValue(example, Event.class);
        System.out.println(e);
        assertEquals(e.getEventId(), 1);
        assertEquals(e.getClass(), NewMessageEvent.class);
        NewMessageEvent newMessageEvent = (NewMessageEvent) e;
        assertEquals(newMessageEvent.getMsgId(), 6811058128403038841L);
        assertEquals(newMessageEvent.getTimestamp(), 1585823048);
        assertEquals(newMessageEvent.getText(), "meh");
        assertEquals(newMessageEvent.getChat().getChatId(), "example@example.ru");
        assertNull(newMessageEvent.getChat().getTitle());
        assertEquals(newMessageEvent.getChat().getType(), "private");
        assertEquals(newMessageEvent.getFrom().getFirstName(), "Данил");
        assertNull(newMessageEvent.getFrom().getLastName());
        assertNull(newMessageEvent.getFrom().getNick());
        assertEquals(newMessageEvent.getFrom().getUserId(), "example@example.ru");
        assertNull(newMessageEvent.getParts());
    }

    // TODO написать нормальный тест
    @Ignore
    public void deserializationNewMessageWithReplyEventTest() throws IOException {
        String example3 = "{\"ok\":true,\"events\":[{\"eventId\":2,\"payload\":{\"chat\":{\"chatId\":\"example@example.ru\",\"type\":\"private\"},\"parts\":[{\"payload\":{\"message\":{\"msgId\":\"6811210290504401070\",\"from\":{\"nick\":\"OnlyMineAgentBot\",\"firstName\":\"OnlyMineAgentBot\",\"userId\":\"751619011\"},\"text\":\"kek\",\"timestamp\":1585858476}},\"type\":\"reply\"}],\"msgId\":\"6811297959376846925\",\"from\":{\"firstName\":\"Данил\",\"userId\":\"example@example.ru\"},\"text\":\"LA KEK!\",\"timestamp\":1585878888},\"type\":\"newMessage\"}]}";
        org.codehaus.jackson.map.ObjectMapper objectMapper = new org.codehaus.jackson.map.ObjectMapper();
        System.out.println(objectMapper.readTree(example3));
        FetchResponseDto fetchResponseDto = objectMapper.readValue(example3, FetchResponseDto.class);
        System.out.println(fetchResponseDto);
    }

}