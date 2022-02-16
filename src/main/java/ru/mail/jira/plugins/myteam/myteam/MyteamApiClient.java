/* (C)2020 */
package ru.mail.jira.plugins.myteam.myteam;

import java.io.IOException;
import java.util.List;
import javax.annotation.Nonnull;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.UnirestException;
import ru.mail.jira.plugins.myteam.exceptions.MyteamServerErrorException;
import ru.mail.jira.plugins.myteam.myteam.dto.*;
import ru.mail.jira.plugins.myteam.myteam.dto.chats.*;
import ru.mail.jira.plugins.myteam.myteam.dto.response.*;

public interface MyteamApiClient {
  HttpResponse<MessageResponse> sendMessageText(
      String chatId, String text, List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException;

  HttpResponse<MessageResponse> sendMessageText(String chatId, String text)
      throws UnirestException, IOException, MyteamServerErrorException;

  HttpResponse<StatusResponse> deleteMessages(String chatId, List<Long> messagesId)
      throws UnirestException, IOException, MyteamServerErrorException;

  HttpResponse<FetchResponse> getEvents(long lastEventId, long pollTime)
      throws UnirestException, MyteamServerErrorException;

  HttpResponse<AdminsResponse> getAdmins(String chatId)
      throws UnirestException, MyteamServerErrorException;

  HttpResponse<JsonNode> answerCallbackQuery(
      String queryId, String text, boolean showAlert, String url)
      throws UnirestException, MyteamServerErrorException;

  HttpResponse<JsonNode> answerCallbackQuery(String queryId)
      throws UnirestException, MyteamServerErrorException;

  void updateSettings();

  HttpResponse<FileResponse> getFile(String fileId)
      throws UnirestException, MyteamServerErrorException;

  HttpResponse<MessageResponse> editMessageText(
      String chatId,
      long messageId,
      String text,
      List<List<InlineKeyboardMarkupButton>> inlineKeyboardMarkup)
      throws UnirestException, IOException, MyteamServerErrorException;

  HttpResponse<CreateChatResponse> createChat(
      @Nonnull String creatorBotToken,
      @Nonnull String name,
      String about,
      @Nonnull List<ChatMemberId> members,
      boolean isPublic)
      throws IOException, UnirestException, MyteamServerErrorException;

  HttpResponse<SuccessResponse> setAboutChat(
      @Nonnull String botToken, @Nonnull String chatId, String about)
      throws IOException, UnirestException, MyteamServerErrorException;

  HttpResponse<ChatInfoResponse> getChatInfo(@Nonnull String botToken, @Nonnull String chatId)
      throws UnirestException, MyteamServerErrorException;

  HttpResponse<ChatMember> getMembers(@Nonnull String chatId) throws UnirestException;

  HttpResponse<BotMetaInfo> getSelfInfo() throws UnirestException, MyteamServerErrorException;
}
