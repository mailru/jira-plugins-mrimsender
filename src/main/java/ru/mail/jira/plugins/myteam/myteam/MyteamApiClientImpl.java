/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.body.MultipartBody;
import java.io.IOException;
import java.util.List;
import org.apache.http.entity.ContentType;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.myteam.model.PluginData;
import ru.mail.jira.plugins.myteam.myteam.dto.*;

@Component
public class MyteamApiClientImpl implements MyteamApiClient {
  private String apiToken;
  private String botApiUrl;
  private final ObjectMapper objectMapper;
  private final PluginData pluginData;

  @Autowired
  public MyteamApiClientImpl(PluginData pluginData) {
    this.objectMapper = new ObjectMapper();
    this.pluginData = pluginData;
    this.apiToken = pluginData.getToken();
    this.botApiUrl = pluginData.getBotApiUrl();
  }

  @Override
  public void updateSettings() {
    this.apiToken = pluginData.getToken();
    this.botApiUrl = pluginData.getBotApiUrl();
  }

  @Override
  public HttpResponse<MessageResponse> sendMessageText(
      String chatId, String text, List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException {
    if (inlineKeyboardMarkup == null)
      return Unirest.post(botApiUrl + "/messages/sendText")
          .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
          .field("token", apiToken)
          .field("chatId", chatId)
          .field("text", text)
          .asObject(MessageResponse.class);
    return Unirest.post(botApiUrl + "/messages/sendText")
        .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
        .field("token", apiToken)
        .field("chatId", chatId)
        .field("text", text)
        .field("inlineKeyboardMarkup", objectMapper.writeValueAsString(inlineKeyboardMarkup))
        .asObject(MessageResponse.class);
  }

  @Override
  public HttpResponse<MessageResponse> sendMessageText(String chatId, String text)
      throws IOException, UnirestException {
    return sendMessageText(chatId, text, null);
  }

  @Override
  public HttpResponse<FetchResponseDto> getEvents(long lastEventId, long pollTime)
      throws UnirestException {
    return Unirest.get(botApiUrl + "/events/get")
        .queryString("token", apiToken)
        .queryString("lastEventId", lastEventId)
        .queryString("pollTime", pollTime)
        .asObject(FetchResponseDto.class);
  }

  @Override
  public HttpResponse<JsonNode> answerCallbackQuery(
      String queryId, String text, boolean showAlert, String url) throws UnirestException {
    return Unirest.get(botApiUrl + "/messages/answerCallbackQuery")
        .queryString("token", apiToken)
        .queryString("queryId", queryId)
        .queryString("text", text)
        .queryString("showAlert", showAlert)
        .queryString("url", url)
        .asJson();
  }

  @Override
  public HttpResponse<JsonNode> answerCallbackQuery(String queryId) throws UnirestException {
    return answerCallbackQuery(queryId, null, false, null);
  }

  @Override
  public HttpResponse<FileResponse> getFile(String fileId) throws UnirestException {
    return Unirest.get(botApiUrl + "/files/getInfo")
        .queryString("token", apiToken)
        .queryString("fileId", fileId)
        .asObject(FileResponse.class);
  }

  @Override
  public HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException {
    if (inlineKeyboardMarkup == null)
      return Unirest.post(botApiUrl + "/messages/editText")
          .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
          .field("token", apiToken)
          .field("chatId", chatId)
          .field("msgId", messageId)
          .field("text", text)
          .asObject(MessageResponse.class);
    return Unirest.post(botApiUrl + "/messages/editText")
        .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
        .field("token", apiToken)
        .field("chatId", chatId)
        .field("msgId", messageId)
        .field("text", text)
        .field("inlineKeyboardMarkup", objectMapper.writeValueAsString(inlineKeyboardMarkup))
        .asObject(MessageResponse.class);
  }

  @Override
  public HttpResponse<ChatResponse> createChat(
      @NotNull String creatorBotToken,
      @NotNull String name,
      String description,
      @NotNull List<ChatMemberId> members,
      boolean isPublic)
      throws IOException, UnirestException {
    if (members.size() > 0) {
      MultipartBody postBody =
          Unirest.post(botApiUrl + "/chats/createChat")
              .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
              .field("token", creatorBotToken)
              .field("name", name)
              .field("members", objectMapper.writeValueAsString(members));
      if (description != null) postBody.field("description", description);
      return postBody.asObject(ChatResponse.class);
    } else {
      throw new IOException(
          "Error occurred in createChat method: attempt to create chat without eny members");
    }
  }
}
