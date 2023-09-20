/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import kong.unirest.*;
import kong.unirest.apache.ApacheClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.mail.jira.plugins.commons.HttpClient;
import ru.mail.jira.plugins.commons.JacksonObjectMapper;
import ru.mail.jira.plugins.myteam.commons.Utils;
import ru.mail.jira.plugins.myteam.commons.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.BotMetaInfo;
import ru.mail.jira.plugins.myteam.myteam.dto.InlineKeyboardMarkupButton;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.*;
import ru.mail.jira.plugins.myteam.myteam.dto.response.*;
import ru.mail.jira.plugins.myteam.myteam.dto.response.FileResponse;
import ru.mail.jira.plugins.myteam.service.PluginData;

@Slf4j
@Component
@SuppressWarnings("NullAway")
public class MyteamApiClientImpl implements MyteamApiClient {
  private final ObjectMapper objectMapper;
  private final PluginData pluginData;
  private final UnirestInstance retryClient;
  private String apiToken;
  private String botApiUrl;
  private String botId;

  @Autowired
  public MyteamApiClientImpl(PluginData pluginData) {
    this.objectMapper = new ObjectMapper();
    this.pluginData = pluginData;
    this.apiToken = pluginData.getToken();
    this.botApiUrl = pluginData.getBotApiUrl();

    retryClient = Unirest.spawnInstance();

    CloseableHttpClient apacheClient =
        HttpClientBuilder.create()
            .setRetryHandler((exception, executionCount, context) -> executionCount <= 3)
            .setServiceUnavailableRetryStrategy(
                new ServiceUnavailableRetryStrategy() {
                  private long interval = 100;

                  @Override
                  public boolean retryRequest(
                      org.apache.http.HttpResponse response,
                      int executionCount,
                      HttpContext context) {
                    if (executionCount > 3) {
                      return false;
                    }
                    if (response.getStatusLine() == null) {
                      return true;
                    }
                    final int statusCode = response.getStatusLine().getStatusCode();
                    return statusCode == HttpStatus.GATEWAY_TIMEOUT;
                  }

                  @Override
                  public long getRetryInterval() {
                    final long retryInterval = interval;
                    interval *= 2;
                    return retryInterval;
                  }
                })
            .build();

    retryClient
        .config()
        .connectTimeout(2_000)
        .socketTimeout(2_500)
        .setObjectMapper(new JacksonObjectMapper())
        .httpClient(new ApacheClient(apacheClient, retryClient.config()));

    HttpClient.getPrimaryClient().shutDown();
  }

  @Override
  public void updateSettings() {
    this.apiToken = pluginData.getToken();
    this.botApiUrl = pluginData.getBotApiUrl();
    updateBotMetaInfo();
  }

  @Override
  public HttpResponse<MessageResponse> sendMessageText(
      String chatId,
      @Nullable String text,
      @Nullable List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException {

    HttpResponse<MessageResponse> response = sendMessage(chatId, text, inlineKeyboardMarkup, true);

    if (response.getBody() != null
        && !response.getBody().isOk()
        && response.getBody().getDescription().equals("Format error")) {
      response = sendMessage(chatId, Utils.unshieldText(text), inlineKeyboardMarkup, false);
    }

    checkMyteamSendTextErrorException(response, chatId, text);
    return response;
  }

  @Override
  public HttpResponse<MessageResponse> sendMessageText(String chatId, @Nullable String text)
      throws IOException, UnirestException, MyteamServerErrorException {
    return sendMessageText(chatId, text, null);
  }

  @Override
  public HttpResponse<StatusResponse> deleteMessages(String chatId, List<Long> messagesId)
      throws UnirestException, MyteamServerErrorException {
    HttpResponse<StatusResponse> response =
        retryClient
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
        retryClient
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
        retryClient
            .get(botApiUrl + "/chats/getAdmins")
            .queryString("token", apiToken)
            .queryString("chatId", chatId)
            .asObject(AdminsResponse.class);
    checkMyteamServerErrorException(response, "getAdmins");
    return response;
  }

  @Override
  public HttpResponse<JsonNode> answerCallbackQuery(
      String queryId, @Nullable String text, boolean showAlert, @Nullable String url)
      throws UnirestException, MyteamServerErrorException {
    HttpResponse<JsonNode> response =
        retryClient
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
        retryClient
            .get(botApiUrl + "/files/getInfo")
            .queryString("token", apiToken)
            .queryString("fileId", fileId)
            .asObject(FileResponse.class);
    checkMyteamServerErrorException(response, "getFile");
    return response;
  }

  @Override
  public InputStream loadUrlFile(String url) throws UnirestException {
    return new ByteArrayInputStream(HttpClient.getPrimaryClient().get(url).asBytes().getBody());
  }

  @Override
  public HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      @Nullable List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException {
    HttpResponse<MessageResponse> response;
    if (inlineKeyboardMarkup == null)
      response =
          retryClient
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
          retryClient
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
      String creatorBotToken,
      String name,
      String about,
      List<ChatMemberId> members,
      boolean isPublic)
      throws IOException, UnirestException, MyteamServerErrorException {
    if (members.size() > 0 && members.size() <= 30) {
      MultipartBody postBody =
          retryClient
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
  public HttpResponse<SuccessResponse> setAboutChat(
      @Nonnull String botToken, String chatId, String about)
      throws UnirestException, MyteamServerErrorException {
    HttpResponse<SuccessResponse> response =
        retryClient
            .get(botApiUrl + "/chats/setAbout")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
            .queryString("token", botToken)
            .queryString("chatId", chatId)
            .queryString("about", about)
            .asObject(SuccessResponse.class);
    checkMyteamServerErrorException(response, "setAboutChat");
    return response;
  }

  @Override
  public HttpResponse<ChatInfoResponse> getChatInfo(@Nonnull String chatId)
      throws UnirestException, MyteamServerErrorException {
    HttpResponse<ChatInfoResponse> response =
        retryClient
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
      HttpResponse<MessageResponse> response, String chatId, @Nullable String text)
      throws MyteamServerErrorException {

    if (response.getStatus() >= 500 || response.getBody() == null || !response.getBody().isOk()) {
      Exception parsingException;
      try {
        parsingException = response.getParsingError().orElse(null);
      } catch (Exception e) {
        parsingException = e;
      }
      MyteamServerErrorException newException =
          new MyteamServerErrorException(
              response.getStatus(),
              String.format(
                  "Caused exception due sending message\n\nchatId: %s\nerror: %s\n%s message\n\n",
                  chatId,
                  response.getBody() != null
                      ? response.getBody().getDescription()
                      : (parsingException instanceof UnirestParsingException
                          ? response
                              .getParsingError()
                              .map(UnirestParsingException::getOriginalBody)
                              .orElse("")
                          : parsingException != null ? parsingException.getLocalizedMessage() : null),
                  text),
              parsingException);
      log.error(
          "Error: {} while sending the message:\n{}",
          response.getBody() != null
              ? response.getBody().getDescription()
              : response
                  .getParsingError()
                  .map(UnirestParsingException::getOriginalBody)
                  .orElse(null),
          text,
          newException);
      throw newException;
    }
  }

  @Override
  public HttpResponse<ChatMember> getMembers(@Nonnull String chatId) throws UnirestException {
    return retryClient
        .post(botApiUrl + "/chats/getMembers")
        .header("Accept", "application/json")
        .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
        .field("token", apiToken)
        .field("chatId", chatId)
        .asObject(ChatMember.class);
  }

  @Override
  public HttpResponse<BotMetaInfo> getSelfInfo()
      throws UnirestException, MyteamServerErrorException {
    return retryClient
        .post(botApiUrl + "/self/get")
        .header("Accept", "application/json")
        .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
        .field("token", apiToken)
        .asObject(BotMetaInfo.class);
  }

  @Override
  public String getBotId() {
    if (botId == null) {
      updateBotMetaInfo();
    }
    return botId;
  }

  private void updateBotMetaInfo() {
    try {
      BotMetaInfo botMetaInfo = getSelfInfo().getBody();
      botId = botMetaInfo.getUserId();
    } catch (MyteamServerErrorException e) {
      log.error("Unable to get bot self meta data", e);
    }
  }

  private HttpResponse<MessageResponse> sendMessage(
      String chatId,
      @Nullable String text,
      @Nullable List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup,
      boolean isMarkdown)
      throws IOException {
    MultipartBody req =
        retryClient
            .post(botApiUrl + "/messages/sendText")
            .header("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
            .field("token", apiToken)
            .field("chatId", chatId)
            .field("text", text);

    if (isMarkdown) {
      req.field("parseMode", "MarkdownV2");
    }

    if (inlineKeyboardMarkup != null) {
      req.field("inlineKeyboardMarkup", objectMapper.writeValueAsString(inlineKeyboardMarkup));
    }
    return req.asObject(MessageResponse.class);
  }
}
