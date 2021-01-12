/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import ru.mail.jira.plugins.myteam.myteam.dto.*;

public interface MyteamApiClient {
  HttpResponse<MessageResponse> sendMessageText(
      String chatId, String text, List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException;

  HttpResponse<MessageResponse> sendMessageText(String chatId, String text)
      throws UnirestException, IOException;

  HttpResponse<FetchResponseDto> getEvents(long lastEventId, long pollTime) throws UnirestException;

  HttpResponse<JsonNode> answerCallbackQuery(
      String queryId, String text, boolean showAlert, String url) throws UnirestException;

  HttpResponse<JsonNode> answerCallbackQuery(String queryId) throws UnirestException;

  void updateSettings();

  HttpResponse<FileResponse> getFile(String fileId) throws UnirestException;

  HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException;

  HttpResponse<ChatResponse> createChat(
      @Nonnull String creatorBotToken,
      @Nonnull String name,
      String description,
      @Nonnull List<ChatMemberId> members,
      boolean isPublic)
      throws IOException, UnirestException;
}
