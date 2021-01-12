/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Ignore;
import org.mockito.Mockito;
import ru.mail.jira.plugins.myteam.model.PluginData;
import ru.mail.jira.plugins.myteam.myteam.dto.FetchResponseDto;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.MessageResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.events.CallbackQueryEvent;

public class MyteamApiClientImplTest {

  private PluginData pluginData;
  private MyteamApiClient myteamApiClient;

  @Before
  public void setUp() throws Exception {
    // Mocking MyteamApiClient object
    try (InputStream resourceAsStream =
        getClass().getClassLoader().getResourceAsStream("env.properties")) {
      Properties properties = new Properties();
      if (resourceAsStream == null) {
        System.out.println("env.properties file not found !");
        return;
      }
      properties.load(resourceAsStream);
      this.pluginData = Mockito.mock(PluginData.class);
      when(pluginData.getToken()).thenReturn(properties.getProperty("myteam.test.bot.token"));
      when(pluginData.getBotApiUrl()).thenReturn("https://api.internal.myteam.mail.ru/bot/v1");
      this.myteamApiClient = new MyteamApiClientImpl(this.pluginData);
    } catch (IOException ioException) {
      ioException.printStackTrace();
    }

    // unirest initialization
    Unirest.setTimeouts(10_000, 300_000);
    Unirest.setObjectMapper(
        new ObjectMapper() {
          private org.codehaus.jackson.map.ObjectMapper jacksonObjectMapper =
              new org.codehaus.jackson.map.ObjectMapper();

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
  }

  @Ignore
  public void sendMessageText() throws IOException, UnirestException {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>(1);
    buttons.add(new ArrayList<>(2));
    buttons
        .get(0)
        .add(InlineKeyboardMarkupButton.buildButtonWithoutUrl("example button1", "callbackData1"));
    buttons
        .get(0)
        .add(InlineKeyboardMarkupButton.buildButtonWithoutUrl("example button 2", "callbackData2"));
    HttpResponse<MessageResponse> httpResponse =
        myteamApiClient.sendMessageText(
            "d.udovichenko@corp.mail.ru", "hello from sendMessageText() test", buttons);

    assertTrue(httpResponse.getBody().isOk());
  }

  @Ignore
  public void editMessageText() throws IOException, UnirestException {
    List<List<InlineKeyboardMarkupButton>> buttons = new ArrayList<>(1);
    buttons.add(new ArrayList<>(2));
    buttons
        .get(0)
        .add(InlineKeyboardMarkupButton.buildButtonWithoutUrl("example button1", "callbackData1"));
    buttons
        .get(0)
        .add(InlineKeyboardMarkupButton.buildButtonWithoutUrl("example button 2", "callbackData2"));

    HttpResponse<MessageResponse> sendMessageResponse =
        myteamApiClient.sendMessageText(
            "d.udovichenko@corp.mail.ru", "hello from editMessageText() test", buttons);
    long sentMsgId = sendMessageResponse.getBody().getMsgId();

    HttpResponse<MessageResponse> editMessageResponse =
        myteamApiClient.editMessageText(
            "d.udovichenko@corp.mail.ru",
            sentMsgId,
            "edited message from editMessageText() test",
            buttons);
    assertTrue(editMessageResponse.getBody().isOk());
  }

  @Ignore
  public void getEvents() throws UnirestException {
    HttpResponse<FetchResponseDto> httpResponse = myteamApiClient.getEvents(0, 5);
    assertEquals(200, httpResponse.getStatus());
  }

  @Ignore
  public void answerCallbackQuery() throws UnirestException {
    HttpResponse<FetchResponseDto> httpResponse = myteamApiClient.getEvents(0, 5);
    List<CallbackQueryEvent> callbackQueryEventList =
        httpResponse.getBody().getEvents().stream()
            .filter(event -> event instanceof CallbackQueryEvent)
            .map(event -> (CallbackQueryEvent) event)
            .collect(Collectors.toList());
    if (callbackQueryEventList.size() > 0) {
      CallbackQueryEvent callbackQueryEvent = callbackQueryEventList.get(0);
      HttpResponse<JsonNode> jsonNodeHttpResponse =
          myteamApiClient.answerCallbackQuery(
              callbackQueryEvent.getQueryId(), "text here", true, null);
      assertEquals(200, jsonNodeHttpResponse.getStatus());
    }
  }
}
