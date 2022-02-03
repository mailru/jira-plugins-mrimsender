/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import ru.mail.jira.plugins.commons.HttpClient;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.FetchResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.events.CallbackQueryEvent;
import ru.mail.jira.plugins.myteam.myteam.dto.events.IcqEvent;
import ru.mail.jira.plugins.myteam.myteam.dto.events.NewMessageEvent;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.File;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Forward;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Mention;
import ru.mail.jira.plugins.myteam.myteam.dto.parts.Part;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Ignore
// todo
public class MyteamEventsFetcherTest {
  private PluginData pluginData;
  private MyteamApiClient myteamApiClient;
  private org.codehaus.jackson.map.ObjectMapper jacksonObjectMapper;

  @Before
  public void init() throws IOException {
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
      when(pluginData.getBotApiUrl()).thenReturn(properties.getProperty("myteam.test.bot.api"));
      this.myteamApiClient = new MyteamApiClientImpl(this.pluginData);
    }

    // unirest initialization
    jacksonObjectMapper = new org.codehaus.jackson.map.ObjectMapper();
    HttpClient.init();
  }

  @Test
  public void fetchIcqEvents() throws UnirestException, MyteamServerErrorException {
    HttpResponse<FetchResponse> eventHttpResponse = this.myteamApiClient.getEvents(0, 5);
    System.out.println(eventHttpResponse.getBody());
    eventHttpResponse.getBody().getEvents().forEach(event -> System.out.println(event.toString()));
  }

  @Test
  public void deserializationFetchResponseTest() throws IOException {
    String example =
        "{\"ok\":true,\"events\":[{\"eventId\":1,\"payload\":{\"chat\":{\"chatId\":\"example@example.ru\",\"type\":\"private\"},\"msgId\":\"6811058128403038841\",\"from\":{\"firstName\":\"Данил\",\"userId\":\"example@example.ru\"},\"text\":\"meh\",\"timestamp\":1585823048},\"type\":\"newMessage\"},{\"eventId\":1,\"payload\":{\"chat\":{\"chatId\":\"example@example.ru\",\"type\":\"private\"},\"msgId\":\"6811058128403038841\",\"from\":{\"firstName\":\"Данил\",\"userId\":\"example@example.ru\"},\"text\":\"meh\",\"timestamp\":1585823048},\"type\":\"faksdmfl\"}, {\"eventId\":5,\"payload\":{\"callbackData\":\"next-page1\",\"from\":{\"firstName\":\"Данил\",\"userId\":\"example@example.ru\"},\"message\":{\"chat\":{\"chatId\":\"example@example.ru\",\"type\":\"private\"},\"parts\":[{\"payload\":[[{\"callbackData\":\"next-page1\",\"text\":\"asdad1\"},{\"callbackData\":\"next-page2\",\"text\":\"asdad2\"}],[{\"callbackData\":\"next-page3\",\"text\":\"asdad3\"},{\"callbackData\":\"next-page4\",\"text\":\"asdad4\"}]],\"type\":\"inlineKeyboardMarkup\"}],\"msgId\":\"6812931455698600506\",\"from\":{\"nick\":\"OnlyMineAgentBot\",\"firstName\":\"OnlyMineAgentBot\",\"userId\":\"751619011\"},\"text\":\"kek\",\"timestamp\":1586259216},\"queryId\":\"SVR:example@example.ru:751619011:1586266646713388:333-1586266647\"},\"type\":\"callbackQuery\"}]}\n";
    FetchResponse fetchResponseDto =
        jacksonObjectMapper.readValue(example, FetchResponse.class);
    assertTrue(fetchResponseDto.isOk());
    assertEquals(3, fetchResponseDto.getEvents().size());
    assertEquals(NewMessageEvent.class, fetchResponseDto.getEvents().get(0).getClass());
    assertEquals(IcqEvent.class, fetchResponseDto.getEvents().get(1).getClass());
    assertEquals(CallbackQueryEvent.class, fetchResponseDto.getEvents().get(2).getClass());
  }

  @Test
  public void deserializationFetchResponseTestForParts() throws IOException {
    String example =
        "{\"events\": [{\"eventId\": 183, \"payload\": {\"chat\": {\"chatId\": \"example@example.ru\", \"type\": \"private\"}, \"from\": {\"firstName\": \"Данил\", \"userId\": \"example@example.ru\"}, \"msgId\": \"6816094467183870357\", \"text\": \"yuio\", \"timestamp\": 1586995662}, \"type\": \"newMessage\"}, {\"eventId\": 184, \"payload\": {\"chat\": {\"chatId\": \"example@example.ru\", \"type\": \"private\"}, \"from\": {\"firstName\": \"Данил\", \"userId\": \"example@example.ru\"}, \"msgId\": \"6816094643277529285\", \"parts\": [{\"payload\": {\"fileId\": \"28484BzdUKPEEwmHDf4DmI5af94aff1ac\"}, \"type\": \"sticker\"}], \"text\": \"https://files.icq.net/get/28484BzdUKPEEwmHDf4DmI5af94aff1ac\", \"timestamp\": 1586995703}, \"type\": \"newMessage\"}, {\"eventId\": 185, \"payload\": {\"chat\": {\"chatId\": \"example@example.ru\", \"type\": \"private\"}, \"from\": {\"firstName\": \"Данил\", \"userId\": \"example@example.ru\"}, \"msgId\": \"6816094939630273234\", \"parts\": [{\"payload\": {\"fileId\": \"0DSdI000zoo7OPpedKDs0a5e97a23c1ab\", \"type\": \"image\"}, \"type\": \"file\"}], \"text\": \"https://files.icq.net/get/0DSdI000zoo7OPpedKDs0a5e97a23c1ab\", \"timestamp\": 1586995772}, \"type\": \"newMessage\"}, {\"eventId\": 186, \"payload\": {\"chat\": {\"chatId\": \"example@example.ru\", \"type\": \"private\"}, \"from\": {\"firstName\": \"Данил\", \"userId\": \"example@example.ru\"}, \"msgId\": \"6816095248867917825\", \"parts\": [{\"payload\": {\"firstName\": \"OnlyMineAgentBot\", \"nick\": \"OnlyMineAgentBot\", \"userId\": \"751619011\"}, \"type\": \"mention\"}], \"text\": \"@[751619011]  here i am\", \"timestamp\": 1586995844}, \"type\": \"newMessage\"}, {\"eventId\": 81, \"payload\": {\"chat\": {\"chatId\": \"d.udovichenko@corp.mail.ru\", \"type\": \"private\"}, \"from\": {\"firstName\": \"Данил\", \"lastName\": \"Удовиченко\", \"userId\": \"d.udovichenko@corp.mail.ru\"}, \"msgId\": \"6865294137898304021\", \"parts\": [{\"payload\": {\"message\": {\"from\": {\"firstName\": \"Metabot\", \"nick\": \"metabot\", \"userId\": \"70001\"}, \"msgId\": \"6865292565940273485\", \"text\": \"Please enter botId or nick.\", \"timestamp\": 1598450487}}, \"type\": \"forward\"}], \"timestamp\": 1598450853}, \"type\": \"newMessage\"}], \"ok\": true}";
    FetchResponse fetchResponseDto =
        jacksonObjectMapper.readValue(example, FetchResponse.class);
    assertTrue(fetchResponseDto.isOk());
    assertEquals(5, fetchResponseDto.getEvents().size());
    fetchResponseDto
        .getEvents()
        .forEach(event -> assertEquals(NewMessageEvent.class, event.getClass()));
    List<NewMessageEvent> newMessageEvents =
        fetchResponseDto.getEvents().stream()
            .map(event -> (NewMessageEvent) event)
            .collect(Collectors.toList());
    assertNull(newMessageEvents.get(0).getParts());
    assertEquals(Part.class, newMessageEvents.get(1).getParts().get(0).getClass());
    assertEquals(File.class, newMessageEvents.get(2).getParts().get(0).getClass());
    assertEquals(Mention.class, newMessageEvents.get(3).getParts().get(0).getClass());
    assertEquals(Forward.class, newMessageEvents.get(4).getParts().get(0).getClass());
    System.out.println(newMessageEvents.get(4).getParts().get(0).toString());
  }

  @Test
  public void deserializationNewMessageEventTest() throws IOException {
    String example =
        "{\"eventId\":1,\"payload\":{\"chat\":{\"chatId\":\"example@example.ru\",\"type\":\"private\"},\"msgId\":\"6811058128403038841\",\"from\":{\"firstName\":\"Данил\",\"userId\":\"example@example.ru\"},\"text\":\"meh\",\"timestamp\":1585823048},\"type\":\"newMessage\"}";

    IcqEvent<?> e = jacksonObjectMapper.readValue(example, IcqEvent.class);
    assertEquals(1, e.getEventId());
    assertEquals(e.getClass(), NewMessageEvent.class);
    NewMessageEvent newMessageEvent = (NewMessageEvent) e;
    assertEquals(6811058128403038841L, newMessageEvent.getMsgId());
    assertEquals(1585823048, newMessageEvent.getTimestamp());
    assertEquals("meh", newMessageEvent.getText());
    assertEquals("example@example.ru", newMessageEvent.getChat().getChatId());
    assertNull(newMessageEvent.getChat().getTitle());
    assertEquals("private", newMessageEvent.getChat().getType());
    assertEquals("Данил", newMessageEvent.getFrom().getFirstName());
    assertNull(newMessageEvent.getFrom().getLastName());
    assertNull(newMessageEvent.getFrom().getNick());
    assertEquals("example@example.ru", newMessageEvent.getFrom().getUserId());
  }

  @Test
  public void deserializationCallbackQueryEventWithManyButtonsTest() throws IOException {
    String example =
        "{\"eventId\":5,\"payload\":{\"callbackData\":\"next-page1\",\"from\":{\"firstName\":\"Данил\",\"userId\":\"example@example.ru\"},\"message\":{\"chat\":{\"chatId\":\"example@example.ru\",\"type\":\"private\"},\"parts\":[{\"payload\":[[{\"callbackData\":\"next-page1\",\"text\":\"asdad1\"},{\"callbackData\":\"next-page2\",\"text\":\"asdad2\"}],[{\"callbackData\":\"next-page3\",\"text\":\"asdad3\"},{\"callbackData\":\"next-page4\",\"text\":\"asdad4\"}]],\"type\":\"inlineKeyboardMarkup\"}],\"msgId\":\"6812931455698600506\",\"from\":{\"nick\":\"OnlyMineAgentBot\",\"firstName\":\"OnlyMineAgentBot\",\"userId\":\"751619011\"},\"text\":\"kek\",\"timestamp\":1586259216},\"queryId\":\"SVR:example@example.ru:751619011:1586266646713388:333-1586266647\"},\"type\":\"callbackQuery\"}";
    IcqEvent<?> e = jacksonObjectMapper.readValue(example, IcqEvent.class);
    assertEquals(5, e.getEventId());
    assertEquals(CallbackQueryEvent.class, e.getClass());
    CallbackQueryEvent callbackQueryEvent = (CallbackQueryEvent) e;
    assertEquals(
        "SVR:example@example.ru:751619011:1586266646713388:333-1586266647",
        callbackQueryEvent.getQueryId());
    assertEquals("next-page1", callbackQueryEvent.getCallbackData());
    assertEquals("Данил", callbackQueryEvent.getFrom().getFirstName());
    assertNull(callbackQueryEvent.getFrom().getLastName());
    assertNull(callbackQueryEvent.getFrom().getNick());
    assertEquals("example@example.ru", callbackQueryEvent.getFrom().getUserId());
    assertEquals(1, callbackQueryEvent.getMessage().getParts().size());
    assertEquals("example@example.ru", callbackQueryEvent.getMessage().getChat().getChatId());
    assertEquals("private", callbackQueryEvent.getMessage().getChat().getType());
    assertEquals(6812931455698600506L, callbackQueryEvent.getMessage().getMsgId());
    assertEquals(1586259216, callbackQueryEvent.getMessage().getTimestamp());
    assertEquals("kek", callbackQueryEvent.getMessage().getText());
    assertEquals("751619011", callbackQueryEvent.getMessage().getFrom().getUserId());
    assertEquals("OnlyMineAgentBot", callbackQueryEvent.getMessage().getFrom().getFirstName());
    assertEquals("OnlyMineAgentBot", callbackQueryEvent.getMessage().getFrom().getNick());
  }

  @Ignore
  public void fetcherTest() throws InterruptedException {
    // TODO
  }
}
