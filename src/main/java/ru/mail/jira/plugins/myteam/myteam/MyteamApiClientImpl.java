/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import kong.unirest.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.entity.ContentType;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.HttpClient;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatInfoResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMember;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.ChatMemberId;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.CreateChatResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.response.AdminsResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.response.FetchResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.response.FileResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.response.MessageResponse;
import ru.mail.jira.plugins.myteam.myteam.dto.response.StatusResponse;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Slf4j
@Component
public class MyteamApiClientImpl implements MyteamApiClient {
  private final ObjectMapper objectMapper;
  private final PluginData pluginData;
  private String apiToken;
  private String botApiUrl;

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
      throws UnirestException, IOException, MyteamServerErrorException {
    HttpResponse<MessageResponse> response;
    if (inlineKeyboardMarkup == null)
      response =
          HttpClient.getPrimaryClient()
              .post(botApiUrl + "/messages/sendText")
              .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
              .field("token", apiToken)
              .field("chatId", chatId)
              .field("text", text)
              .field("parseMode", "MarkdownV2")
              .asObject(MessageResponse.class);
    else
      response =
          HttpClient.getPrimaryClient()
              .post(botApiUrl + "/messages/sendText")
              .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
              .field("token", apiToken)
              .field("chatId", chatId)
              .field("text", text)
              .field("parseMode", "MarkdownV2")
              .field("inlineKeyboardMarkup", objectMapper.writeValueAsString(inlineKeyboardMarkup))
              .asObject(MessageResponse.class);
    checkMyteamSendTextErrorException(response, chatId, text);
    return response;
  }

  @Override
  public HttpResponse<MessageResponse> sendMessageText(String chatId, String text)
      throws IOException, UnirestException, MyteamServerErrorException {
    return sendMessageText(chatId, text, null);
  }

  @Override
  public HttpResponse<StatusResponse> deleteMessages(String chatId, List<Long> messagesId)
      throws UnirestException, MyteamServerErrorException {
    HttpResponse<StatusResponse> response =
        HttpClient.getPrimaryClient()
            .get(botApiUrl + "/messages/deleteMessages")
            .queryString("token", apiToken)
            .queryString("chatId", chatId)
            .queryString("msgId", messagesId)
            .asObject(StatusResponse.class);
    checkMyteamServerErrorException(response, "deleteMessages");
    return response;
  }

  @Override
  public HttpResponse<FetchResponse> getEvents(long lastEventId, long pollTime)
      throws UnirestException, MyteamServerErrorException {
    HttpResponse<FetchResponse> response =
        HttpClient.getPrimaryClient()
            .get(botApiUrl + "/events/get")
            .queryString("token", apiToken)
            .queryString("lastEventId", lastEventId)
            .queryString("pollTime", pollTime)
            .asObject(FetchResponse.class);
    checkMyteamServerErrorException(response, "getEvents");
    return response;
  }

  @Override
  public HttpResponse<AdminsResponse> getAdmins(String chatId)
      throws UnirestException, MyteamServerErrorException {
    HttpResponse<AdminsResponse> response =
        HttpClient.getPrimaryClient()
            .get(botApiUrl + "/chats/getAdmins")
            .queryString("token", apiToken)
            .queryString("chatId", chatId)
            .asObject(AdminsResponse.class);
    checkMyteamServerErrorException(response, "getAdmins");
    return response;
  }

  @Override
  public HttpResponse<JsonNode> answerCallbackQuery(
      String queryId, String text, boolean showAlert, String url)
      throws UnirestException, MyteamServerErrorException {
    HttpResponse<JsonNode> response =
        HttpClient.getPrimaryClient()
            .get(botApiUrl + "/messages/answerCallbackQuery")
            .queryString("token", apiToken)
            .queryString("queryId", queryId)
            .queryString("text", text)
            .queryString("showAlert", showAlert)
            .queryString("url", url)
            .asJson();
    checkMyteamServerErrorException(response, "answerCallbackQuery");
    return response;
  }

  @Override
  public HttpResponse<JsonNode> answerCallbackQuery(String queryId)
      throws UnirestException, MyteamServerErrorException {
    return answerCallbackQuery(queryId, null, false, null);
  }

  @Override
  public HttpResponse<FileResponse> getFile(String fileId)
      throws UnirestException, MyteamServerErrorException {
    HttpResponse<FileResponse> response =
        HttpClient.getPrimaryClient()
            .get(botApiUrl + "/files/getInfo")
            .queryString("token", apiToken)
            .queryString("fileId", fileId)
            .asObject(FileResponse.class);
    checkMyteamServerErrorException(response, "getFile");
    return response;
  }

  @Override
  public HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException {
    HttpResponse<MessageResponse> response;
    if (inlineKeyboardMarkup == null)
      response =
          HttpClient.getPrimaryClient()
              .post(botApiUrl + "/messages/editText")
              .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
              .field("token", apiToken)
              .field("chatId", chatId)
              .field("msgId", String.valueOf(messageId))
              .field("text", text)
              .field("parseMode", "MarkdownV2")
              .asObject(MessageResponse.class);
    else
      response =
          HttpClient.getPrimaryClient()
              .post(botApiUrl + "/messages/editText")
              .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
              .field("token", apiToken)
              .field("chatId", chatId)
              .field("msgId", String.valueOf(messageId))
              .field("text", text)
              .field("parseMode", "MarkdownV2")
              .field("inlineKeyboardMarkup", objectMapper.writeValueAsString(inlineKeyboardMarkup))
              .asObject(MessageResponse.class);
    checkMyteamSendTextErrorException(response, chatId, text);
    return response;
  }

  @Override
  public HttpResponse<CreateChatResponse> createChat(
      @NotNull String creatorBotToken,
      @NotNull String name,
      String about,
      @NotNull List<ChatMemberId> members,
      boolean isPublic)
      throws IOException, UnirestException, MyteamServerErrorException {
    if (members.size() > 1 && members.size() <= 30) {
      MultipartBody postBody =
          HttpClient.getPrimaryClient()
              .post(botApiUrl + "/chats/createChat")
              .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
              .field("token", creatorBotToken)
              .field("name", name)
              .field("members", objectMapper.writeValueAsString(members))
              .field("public", String.valueOf(isPublic));
      if (about != null) postBody.field("about", about);
      HttpResponse<CreateChatResponse> response = postBody.asObject(CreateChatResponse.class);
      checkMyteamServerErrorException(response, "createChat");
      return response;
    } else {
      throw new IOException(
          "Error occurred in createChat method: attempt to create chat with not available number of members :"
              + members.size());
    }
  }

  @Override
  public HttpResponse<ChatInfoResponse> getChatInfo(
      @Nonnull String botToken, @Nonnull String chatId)
      throws UnirestException, MyteamServerErrorException {
    HttpResponse<ChatInfoResponse> response =
        HttpClient.getPrimaryClient()
            .post(botApiUrl + "/chats/getInfo")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
            .field("token", apiToken)
            .field("chatId", chatId)
            .asObject(ChatInfoResponse.class);
    checkMyteamServerErrorException(response, "getChatInfo");
    return response;
  }

  void checkMyteamServerErrorException(HttpResponse<?> response, String methodName)
      throws MyteamServerErrorException {
    if (response.getStatus() >= 500 || response.getBody() == null) {
      MyteamServerErrorException newException =
          new MyteamServerErrorException(
              response.getStatus(),
              String.format(
                  "Caused exception due executing method \"%s\"\n%s error\n\n",
                  methodName,
                  response
                      .getParsingError()
                      .map(UnirestParsingException::getOriginalBody)
                      .orElse("")));

      log.error("Myteam server error while {}()", methodName, newException);
      throw newException;
    }
  }

  void checkMyteamSendTextErrorException(
      HttpResponse<MessageResponse> response, String chatId, String text)
      throws MyteamServerErrorException {
    if (response.getStatus() >= 500 || response.getBody() == null || !response.getBody().isOk()) {
      MyteamServerErrorException newException =
          new MyteamServerErrorException(
              response.getStatus(),
              String.format(
                  "Caused exception due sending message\n\nchatId: %s\nerror: %s\n%s message\n\n",
                  chatId, response.getBody().getDescription(), text));
      log.error(
          "Error: {} while sending the message:\n{}",
          response.getBody().getDescription(),
          text,
          newException);
      throw newException;
    }
  }

  @Override
  public HttpResponse<ChatMember> getMembers(@Nonnull String chatId) throws UnirestException {
    return HttpClient.getPrimaryClient()
        .post("https://api.internal.myteam.mail.ru/bot/v1" + "/chats/getMembers")
        .header("Accept", "application/json")
        .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
        .field("token", apiToken)
        .field("chatId", chatId)
        .asObject(ChatMember.class);
  }
}
